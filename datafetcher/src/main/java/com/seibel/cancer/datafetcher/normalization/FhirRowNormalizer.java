package com.seibel.cancer.datafetcher.normalization;

import com.seibel.cancer.common.domain.LabResult;
import com.seibel.cancer.common.domain.LabResultComponent;
import com.seibel.cancer.common.domain.StagingRawFhirResource;
import com.seibel.cancer.database.db.service.LabResultComponentDbService;
import com.seibel.cancer.database.db.service.LabResultDbService;
import com.seibel.cancer.database.db.service.StagingRawFhirResourceDbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Normalizes a single staging_raw_fhir_resource row within its own transaction. Split
 * out from FhirNormalizationService (rather than a self-invoked @Transactional method
 * there) so Spring's transactional proxy is actually applied per row - a self-invoked
 * call bypasses the proxy and @Transactional would be silently ignored. Same reason
 * TrialRowNormalizer is a separate bean.
 *
 * Calls *DbService classes in :database directly, never root's *Service layer - the
 * module dependency direction (root -> datafetcher -> :database) makes the reverse a
 * circular dependency.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class FhirRowNormalizer {

    private final List<FhirSourceParser> parsers;
    private final StagingRawFhirResourceDbService stagingDbService;
    private final LabResultDbService labResultDbService;
    private final LabResultComponentDbService labResultComponentDbService;

    @Transactional
    void normalize(StagingRawFhirResource staging) {
        FhirSourceParser parser = parsers.stream()
                .filter(p -> p.supports(staging.getResourceType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No parser for resourceType: " + staging.getResourceType()));

        NormalizedLabResult normalized = parser.parse(staging.getRawPayload());
        LabResult parsed = normalized.labResult();

        // Upsert by Epic's own resource id - the natural key, analogous to nctId.
        LabResult existing = labResultDbService.findByFhirResourceId(parsed.getFhirResourceId());
        LabResult saved;
        if (existing == null) {
            saved = labResultDbService.create(parsed);
        } else {
            saved = labResultDbService.update(existing.getExtid(), parsed);
            // Clean slate rather than diffing, matching the trial normalizer's approach.
            deleteExistingComponents(saved.getId());
        }

        for (LabResultComponent component : normalized.components()) {
            component.setLabResultId(saved.getId());
            labResultComponentDbService.create(component);
        }

        markNormalized(staging);
        log.info("normalize(): fhirResourceId={}, components={}",
                parsed.getFhirResourceId(), normalized.components().size());
    }

    private void deleteExistingComponents(Long labResultId) {
        List<LabResultComponent> existing = labResultComponentDbService.findByLabResultId(labResultId);
        for (LabResultComponent component : existing) {
            labResultComponentDbService.delete(component.getExtid());
        }
    }

    private void markNormalized(StagingRawFhirResource staging) {
        stagingDbService.update(staging.getExtid(), null, null, null, null, LocalDateTime.now(), null);
    }

    /**
     * Records a parse/normalize failure on the staging row itself, so a bad payload is
     * visible rather than silently retried forever. Its own transaction - the failed
     * row's transaction has already rolled back by the time this runs.
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    void markFailed(StagingRawFhirResource staging, String errorMessage) {
        try {
            stagingDbService.update(staging.getExtid(), null, null, null, null, null, truncate(errorMessage));
        } catch (Exception e) {
            log.error("markFailed(): could not record error on staging row extid={}", staging.getExtid(), e);
        }
    }

    private String truncate(String message) {
        if (message == null) {
            return "Unknown normalization error";
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}
