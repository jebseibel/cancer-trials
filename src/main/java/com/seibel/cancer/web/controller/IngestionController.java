package com.seibel.cancer.web.controller;

import com.seibel.cancer.datafetcher.clinicaltrials.ClinicalTrialsGovIngestJob;
import com.seibel.cancer.datafetcher.normalization.TrialNormalizationService;
import com.seibel.cancer.web.request.RequestClinicalTrialsIngest;
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

    private final ClinicalTrialsGovIngestJob ingestJob;
    private final TrialNormalizationService normalizationService;

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
