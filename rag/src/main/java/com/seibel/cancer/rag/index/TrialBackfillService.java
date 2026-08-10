package com.seibel.cancer.rag.index;

import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.common.enums.ActiveEnum;
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

    /**
     * @param pageSize   trials per page - bounds memory and embedding batch size
     * @param maxTrials  stop after this many, for a bounded first run
     */
    public BackfillResult backfillAll(int pageSize, int maxTrials) {
        int trialsIndexed = 0;
        int chunksWritten = 0;
        int trialsSkipped = 0;
        List<String> errors = new ArrayList<>();

        // Checked once up front rather than discovered per trial. Without this a missing
        // collection produces one identical error per trial and still returns "success" with
        // zero indexed, which reads as a data problem instead of a setup problem.
        String setupProblem = indexService.checkVectorStoreReady();
        if (setupProblem != null) {
            log.error("backfill aborted: {}", setupProblem);
            errors.add(setupProblem);
            return new BackfillResult(0, 0, 0, errors);
        }

        int pageNumber = 0;
        Page<Trial> page;
        do {
            page = trialDbService.findByActive(ActiveEnum.ACTIVE, PageRequest.of(pageNumber, pageSize));
            for (Trial trial : page.getContent()) {
                if (trialsIndexed + trialsSkipped >= maxTrials) break;
                try {
                    int written = indexService.reindexTrial(trial.getExtid());
                    if (written == 0) {
                        trialsSkipped++;
                    } else {
                        trialsIndexed++;
                        chunksWritten += written;
                    }
                } catch (Exception e) {
                    // Keep going. One bad trial must not abandon the rest of the corpus -
                    // the whole point of a resumable backfill.
                    log.error("backfill failed for trial extid={} nctId={}",
                            trial.getExtid(), trial.getNctId(), e);
                    errors.add(trial.getNctId() + ": " + e.getMessage());
                }
            }
            pageNumber++;
        } while (page.hasNext() && (trialsIndexed + trialsSkipped) < maxTrials);

        log.info("backfill complete: trialsIndexed={} chunksWritten={} skipped={} errors={}",
                trialsIndexed, chunksWritten, trialsSkipped, errors.size());
        return new BackfillResult(trialsIndexed, chunksWritten, trialsSkipped, errors);
    }

    /**
     * @param trialsIndexed trials that produced at least one chunk
     * @param chunksWritten total chunks embedded and stored
     * @param trialsSkipped trials that produced no chunks (e.g. no eligibility text)
     * @param errors        per-trial failures; the run continues past each one
     */
    public record BackfillResult(int trialsIndexed, int chunksWritten, int trialsSkipped,
                                 List<String> errors) {
    }
}
