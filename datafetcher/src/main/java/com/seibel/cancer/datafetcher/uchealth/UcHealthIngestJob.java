package com.seibel.cancer.datafetcher.uchealth;

import com.fasterxml.jackson.databind.JsonNode;
import com.seibel.cancer.common.domain.StagingRawFhirResource;
import com.seibel.cancer.database.db.service.StagingRawFhirResourceDbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches FHIR resources for the authorized patient and writes them to the staging
 * table. Normalization into readable clinical rows is a separate, later step - this
 * job's only job is landing the raw payload verbatim.
 *
 * Dedup mirrors ClinicalTrialsGovIngestJob: skip rows still pending normalization,
 * refresh already-normalized ones. Built in from the start rather than discovered later.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UcHealthIngestJob {

    private final UcHealthFhirClient fhirClient;
    private final StagingRawFhirResourceDbService stagingDbService;

    /** Fetches and stages every MedicationRequest for the authorized patient. */
    public IngestResult runMedicationRequests() {
        return stage(UcHealthFhirClient.MEDICATION_REQUEST, fhirClient.fetchMedicationRequests());
    }

    /** Fetches and stages the authorized patient's laboratory Observations (test results). */
    public IngestResult runLabObservations() {
        return stage(UcHealthFhirClient.OBSERVATION, fhirClient.fetchLabObservations());
    }

    private IngestResult stage(String resourceType, List<JsonNode> resources) {
        LocalDateTime fetchedAt = LocalDateTime.now();

        int stagedCount = 0;
        int skippedCount = 0;
        List<String> errors = new ArrayList<>();

        for (JsonNode resource : resources) {
            try {
                String fhirResourceId = fhirClient.extractResourceId(resource);
                if (fhirResourceId == null) {
                    errors.add(resourceType + " missing id, skipped");
                    continue;
                }
                String rawPayload = fhirClient.toRawJson(resource);

                StagingRawFhirResource existing = stagingDbService
                        .findByResourceTypeAndFhirResourceId(resourceType, fhirResourceId);

                if (existing == null) {
                    stagingDbService.create(resourceType, fhirResourceId, rawPayload, fetchedAt, null, null);
                    stagedCount++;
                } else if (existing.getNormalizedAt() == null) {
                    // Already staged and still pending normalization - avoid a duplicate row.
                    skippedCount++;
                } else {
                    // Already normalized before - refresh with the latest payload and re-queue it.
                    stagingDbService.refreshForRenormalization(existing.getExtid(), rawPayload, fetchedAt);
                    stagedCount++;
                }
            } catch (Exception e) {
                log.error("Failed to stage {}", resourceType, e);
                errors.add("Failed to stage " + resourceType + ": " + e.getMessage());
            }
        }

        log.info("stage(): resourceType={}, fetched={}, staged={}, skipped={}, errors={}",
                resourceType, resources.size(), stagedCount, skippedCount, errors.size());
        return new IngestResult(resourceType, resources.size(), stagedCount, skippedCount, errors);
    }

    public record IngestResult(String resourceType, int resourcesFetched, int stagingRowsWritten,
                               int stagingRowsSkipped, List<String> errors) {
    }
}
