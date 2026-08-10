package com.seibel.cancer.rag.index;

import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.progress.ProgressTicker;
import com.seibel.cancer.database.db.service.TrialDbService;
import com.seibel.cancer.rag.config.RagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Indexes trials already in MySQL into the vector store.
 *
 * <p>Not a one-off migration script - this is the reconciliation mechanism for the
 * eventual-consistency gap between MySQL and Qdrant (RAG_PLAN.md section 6). It has four
 * standing jobs:
 * <ol>
 *   <li>Index trials ingested before the RAG existed.</li>
 *   <li>Recover trials whose post-commit indexing failed (a Qdrant outage must never roll
 *       back ingested data, so failures leave gaps by design).</li>
 *   <li>Re-index after a chunking change - every existing vector is then stale.</li>
 *   <li>Re-index after an embedding-model change - vectors from different models are not
 *       comparable, so all of them must be regenerated.</li>
 * </ol>
 *
 * <p>Paged rather than {@code findAll()}: the corpus is expected to outgrow memory, and
 * paging makes the work resumable from a known offset if a run dies partway.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrialBackfillService {

    private final TrialDbService trialDbService;
    private final TrialIndexService indexService;
    private final RagProperties ragProperties;

    /** Indexes every active trial, using the configured page size. */
    public BackfillResult backfillAll() {
        return backfillAll(Integer.MAX_VALUE);
    }

    /** Indexes up to maxTrials, using the configured page size. */
    public BackfillResult backfillAll(int maxTrials) {
        return backfillAll(ragProperties.getBackfill().getPageSize(), maxTrials);
    }

    /** Indexes up to maxTrials at the configured page size, optionally re-embedding indexed trials. */
    public BackfillResult backfillAll(int maxTrials, boolean force) {
        return backfillAll(ragProperties.getBackfill().getPageSize(), maxTrials, force);
    }

    /** Indexes up to maxTrials, skipping trials that already have chunks in the store. */
    public BackfillResult backfillAll(int pageSize, int maxTrials) {
        return backfillAll(pageSize, maxTrials, false);
    }

    /**
     * @param pageSize   trials per page - bounds memory and embedding batch size
     * @param maxTrials  stop after this many, for a bounded first run
     * @param force      re-embed trials that are already indexed
     *
     * <p>Trials already holding chunks are skipped by default, which makes topping up a corpus
     * after an ingest cost only the new trials rather than the whole corpus.
     *
     * <p>{@code force} exists because "already indexed" and "correctly indexed" are not the same
     * claim. After a chunking change or an embedding-model swap every stored vector is stale, and
     * skipping on presence would leave the corpus silently mixed - vectors from two models are not
     * comparable, so a partial re-index is worse than none. Jobs 3 and 4 in this class's contract
     * require the override.
     *
     * <p>Indexed state is read from the vector store per trial rather than cached in MySQL. A
     * cached flag would survive a cleared collection and report "indexed" against an empty store -
     * and clearing the collection is routine here, since a database rebuild invalidates every
     * chunk's trial extid.
     */
    public BackfillResult backfillAll(int pageSize, int maxTrials, boolean force) {
        int trialsIndexed = 0;
        int chunksWritten = 0;
        int trialsSkipped = 0;
        int trialsAlreadyIndexed = 0;
        List<String> errors = new ArrayList<>();

        // Checked once up front rather than discovered per trial. Without this a missing
        // collection produces one identical error per trial and still returns "success" with
        // zero indexed, which reads as a data problem instead of a setup problem.
        String setupProblem = indexService.checkVectorStoreReady();
        if (setupProblem != null) {
            log.error("backfill aborted: {}", setupProblem);
            errors.add(setupProblem);
            return new BackfillResult(0, 0, 0, 0, errors);
        }

        // The total is not known until the first page returns, and the ticker needs it for an
        // ETA - so the first query runs before the bar is constructed.
        Page<Trial> page = trialDbService.findByActive(ActiveEnum.ACTIVE, PageRequest.of(0, pageSize));
        int total = (int) Math.min(page.getTotalElements(), maxTrials);

        RagProperties.Progress progressConfig = ragProperties.getProgress();
        try (ProgressTicker ticker = new ProgressTicker(
                "PREPARING FOR SEARCH",
                progressConfig.getLineWidth(),
                progressConfig.getFlushInterval(),
                progressConfig.resolveEnabled(System.console() != null),
                total)) {

            int pageNumber = 0;
            do {
                if (pageNumber > 0) {
                    page = trialDbService.findByActive(
                            ActiveEnum.ACTIVE, PageRequest.of(pageNumber, pageSize));
                }
                for (Trial trial : page.getContent()) {
                    if (trialsIndexed + trialsSkipped + trialsAlreadyIndexed >= maxTrials) break;
                    try {
                        if (!force && indexService.isIndexed(trial.getExtid())) {
                            trialsAlreadyIndexed++;
                            // A skip glyph, not a tick: an already-indexed trial did no work,
                            // and a resumed run is mostly these. Showing them as successes
                            // would make a 30-second resume look like a full re-embed.
                            ticker.skip();
                            continue;
                        }
                        int written = indexService.reindexTrial(trial.getExtid());
                        if (written == 0) {
                            trialsSkipped++;
                            ticker.skip();
                        } else {
                            trialsIndexed++;
                            chunksWritten += written;
                            ticker.tick();
                        }
                    } catch (Exception e) {
                        // Keep going. One bad trial must not abandon the rest of the corpus -
                        // the whole point of a resumable backfill.
                        //
                        // Logged at debug, not error: a mid-loop log line shreds the bar, and
                        // the failure is already carried out in the returned errors list and
                        // counted in the summary below.
                        log.debug("backfill failed for trial extid={} nctId={}",
                                trial.getExtid(), trial.getNctId(), e);
                        errors.add(trial.getNctId() + ": " + e.getMessage());
                        ticker.error();
                    }
                }
                pageNumber++;
            } while (page.hasNext()
                    && (trialsIndexed + trialsSkipped + trialsAlreadyIndexed) < maxTrials);
        }

        log.info("backfill complete: trialsIndexed={} chunksWritten={} skipped={} "
                        + "alreadyIndexed={} errors={}",
                trialsIndexed, chunksWritten, trialsSkipped, trialsAlreadyIndexed, errors.size());
        return new BackfillResult(trialsIndexed, chunksWritten, trialsSkipped,
                trialsAlreadyIndexed, errors);
    }

    /**
     * @param trialsIndexed       trials that produced at least one chunk
     * @param chunksWritten       total chunks embedded and stored
     * @param trialsSkipped       trials that produced no chunks (e.g. no eligibility text)
     * @param trialsAlreadyIndexed trials that already held chunks and were left alone
     * @param errors              per-trial failures; the run continues past each one
     *
     * <p>The two skip counts are reported separately because they mean opposite things: nothing
     * to index versus nothing to do. Collapsing them hides a corpus that produced no chunks at
     * all behind a number that reads like successful deduplication.
     */
    public record BackfillResult(int trialsIndexed, int chunksWritten, int trialsSkipped,
                                 int trialsAlreadyIndexed, List<String> errors) {
    }
}
