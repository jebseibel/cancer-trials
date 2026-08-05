package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Result of a UCHealth/Epic FHIR ingestion run: fetch -> stage -> normalize. Separate
 * from ResponseIngestionResult because that one is trial-shaped
 * (studiesFetched/trialsNormalized).
 */
@Data
@Builder
public class ResponseFhirIngestionResult {
    private String resourceType;
    private int resourcesFetched;
    private int stagingRowsWritten;
    private int stagingRowsSkipped;
    private int pendingRowsProcessed;
    private int resourcesNormalized;
    private List<String> ingestErrors;
    private List<String> normalizationErrors;
}
