package com.seibel.cancer.web.controller;

import com.seibel.cancer.datafetcher.clinicaltrials.ClinicalTrialsGovIngestJob;
import com.seibel.cancer.datafetcher.normalization.FhirNormalizationService;
import com.seibel.cancer.datafetcher.normalization.TrialNormalizationService;
import com.seibel.cancer.datafetcher.uchealth.UcHealthIngestJob;
import com.seibel.cancer.web.request.RequestClinicalTrialsIngest;
import com.seibel.cancer.web.response.ResponseFhirIngestionResult;
import com.seibel.cancer.web.response.ResponseIngestionResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ingestion")
@Validated
@Tag(name = "Ingestion", description = "On-demand trial data ingestion")
@RequiredArgsConstructor
public class IngestionController {

    /** Upper bound on staging rows normalized per ingestion call. */
    private static final int MAX_NORMALIZE_ROWS = 500;

    private final ClinicalTrialsGovIngestJob ingestJob;
    private final TrialNormalizationService normalizationService;
    private final UcHealthIngestJob ucHealthIngestJob;
    private final FhirNormalizationService fhirNormalizationService;

    @PostMapping("/uchealth/observation")
    @Operation(summary = "Fetch the authorized patient's lab Observations, stage them, and normalize into lab_result")
    public ResponseFhirIngestionResult ingestUcHealthObservations() {
        var result = ucHealthIngestJob.runLabObservations();
        var normalizationResult = fhirNormalizationService.normalizePending(MAX_NORMALIZE_ROWS);

        return ResponseFhirIngestionResult.builder()
                .resourceType(result.resourceType())
                .resourcesFetched(result.resourcesFetched())
                .stagingRowsWritten(result.stagingRowsWritten())
                .stagingRowsSkipped(result.stagingRowsSkipped())
                .pendingRowsProcessed(normalizationResult.pendingRows())
                .resourcesNormalized(normalizationResult.normalizedCount())
                .ingestErrors(result.errors())
                .normalizationErrors(normalizationResult.errors())
                .build();
    }

    @PostMapping("/uchealth/medicationrequest")
    @Operation(summary = "Fetch the authorized patient's MedicationRequests from UCHealth/Epic into staging")
    public ResponseFhirIngestionResult ingestUcHealthMedicationRequests() {
        var result = ucHealthIngestJob.runMedicationRequests();

        // Stops at staging on purpose - the FHIR parser/normalizer doesn't exist yet.
        return ResponseFhirIngestionResult.builder()
                .resourceType(result.resourceType())
                .resourcesFetched(result.resourcesFetched())
                .stagingRowsWritten(result.stagingRowsWritten())
                .stagingRowsSkipped(result.stagingRowsSkipped())
                .ingestErrors(result.errors())
                .build();
    }

    @PostMapping("/clinicaltrials")
    @Operation(summary = "Fetch trials from ClinicalTrials.gov, stage them, and normalize into the core schema")
    public ResponseIngestionResult ingestClinicalTrials(@Valid @RequestBody RequestClinicalTrialsIngest request) {
        var ingestResult = ingestJob.run(
                request.getCondition(), request.getTerm(), request.getLocation(), request.getMaxStudies());

        var normalizationResult = normalizationService.normalizePending(request.getMaxStudies());

        return ResponseIngestionResult.builder()
                .studiesFetched(ingestResult.studiesFetched())
                .stagingRowsWritten(ingestResult.stagingRowsWritten())
                .stagingRowsSkipped(ingestResult.stagingRowsSkipped())
                .pendingRowsProcessed(normalizationResult.pendingRows())
                .trialsNormalized(normalizationResult.normalizedCount())
                .ingestErrors(ingestResult.errors())
                .normalizationErrors(normalizationResult.errors())
                .build();
    }
}
