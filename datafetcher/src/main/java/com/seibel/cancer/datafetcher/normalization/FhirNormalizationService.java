package com.seibel.cancer.datafetcher.normalization;

import com.seibel.cancer.common.domain.StagingRawFhirResource;
import com.seibel.cancer.database.db.service.StagingRawFhirResourceDbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Reads pending staging_raw_fhir_resource rows and normalizes each into the clinical
 * schema via FhirRowNormalizer, which owns the per-row transaction and the dispatch to
 * the matching FhirSourceParser.
 *
 * Parallel to TrialNormalizationService but separate: FHIR clinical resources and trials
 * normalize into entirely different tables.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FhirNormalizationService {

    private final StagingRawFhirResourceDbService stagingDbService;
    private final FhirRowNormalizer rowNormalizer;

    public NormalizationResult normalizePending(int maxRows) {
        List<StagingRawFhirResource> pending = stagingDbService.findPending(maxRows);
        int normalizedCount = 0;
        List<String> errors = new ArrayList<>();

        for (StagingRawFhirResource staging : pending) {
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
