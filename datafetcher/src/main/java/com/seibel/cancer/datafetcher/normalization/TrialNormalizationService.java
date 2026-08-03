package com.seibel.cancer.datafetcher.normalization;

import com.seibel.cancer.common.domain.StagingRawTrial;
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

    public NormalizationResult normalizePending(int maxRows) {
        List<StagingRawTrial> pending = stagingRawTrialDbService.findPending(maxRows);
        int normalizedCount = 0;
        List<String> errors = new ArrayList<>();

        for (StagingRawTrial staging : pending) {
            try {
                rowNormalizer.normalize(staging);
                normalizedCount++;
            } catch (Exception e) {
                log.error("Failed to normalize staging row extid={}", staging.getExtid(), e);
                errors.add("extid=" + staging.getExtid() + ": " + e.getMessage());
                rowNormalizer.markFailed(staging, e.getMessage());
            }
        }

        log.info("normalizePending(): pending={}, normalized={}, errors={}",
                pending.size(), normalizedCount, errors.size());
        return new NormalizationResult(pending.size(), normalizedCount, errors);
    }

    public record NormalizationResult(int pendingRows, int normalizedCount, List<String> errors) {
    }
}
