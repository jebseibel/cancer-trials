package com.seibel.cancer.datafetcher.normalization;

import com.seibel.cancer.common.domain.StagingRawTrial;
import com.seibel.cancer.common.progress.ProgressTicker;
import com.seibel.cancer.datafetcher.config.ProgressTickerProperties;
import com.seibel.cancer.database.db.service.StagingRawTrialDbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads pending staging_raw_trial rows and normalizes each into the core schema via
 * TrialRowNormalizer, which handles the per-row transaction, dispatch to the matching
 * TrialSourceParser, and the upsert / delete-and-reinsert-children logic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrialNormalizationService {

    private final StagingRawTrialDbService stagingRawTrialDbService;
    private final TrialRowNormalizer rowNormalizer;
    private final ProgressTickerProperties progressProperties;

    public NormalizationResult normalizePending(int maxRows) {
        List<StagingRawTrial> pending = stagingRawTrialDbService.findPending(maxRows);
        int normalizedCount = 0;
        List<String> errors = new ArrayList<>();

        // Per-run caches for the two lookups that dominate the round-trip count. Measured on a
        // 736-row run: ~3.9 findByName calls per trial for conditions and sponsors, and after the
        // first hundred trials nearly all are repeat hits on the same handful of names. The
        // TrialSource is the same row for every trial in a run, so it was being fetched once per
        // row for no reason.
        //
        // Scoped to one call rather than a Spring @Cacheable bean deliberately: the cache cannot
        // go stale mid-run, and nothing outside this loop can observe it.
        NormalizationCache cache = new NormalizationCache();

        // Errors are logged at debug rather than error here: a log line fired mid-loop lands in
        // the middle of the progress bar and shreds it. Nothing is lost - every failure still
        // goes into the errors list returned below, is counted in the summary line, and is
        // persisted on the staging row by markFailed().
        try (ProgressTicker ticker = new ProgressTicker("NORMALIZING",
                progressProperties.getLineWidth(),
                progressProperties.getFlushInterval(),
                progressProperties.resolveEnabled(System.console() != null),
                pending.size())) {
            for (StagingRawTrial staging : pending) {
                try {
                    rowNormalizer.normalize(staging, cache);
                    normalizedCount++;
                    ticker.tick();
                } catch (Exception e) {
                    log.debug("Failed to normalize staging row extid={}", staging.getExtid(), e);
                    errors.add("extid=" + staging.getExtid() + ": " + e.getMessage());
                    rowNormalizer.markFailed(staging, e.getMessage());
                    ticker.error();
                }
            }
        }

        log.info("normalizePending(): pending={}, normalized={}, errors={}, {}",
                pending.size(), normalizedCount, errors.size(), cache.stats());
        return new NormalizationResult(pending.size(), normalizedCount, errors);
    }

    public record NormalizationResult(int pendingRows, int normalizedCount, List<String> errors) {
    }
}
