package com.seibel.cancer.datafetcher.clinicaltrials;

import com.fasterxml.jackson.databind.JsonNode;
import com.seibel.cancer.common.domain.StagingRawTrial;
import com.seibel.cancer.common.domain.TrialSource;
import com.seibel.cancer.common.progress.ProgressTicker;
import com.seibel.cancer.datafetcher.config.ProgressTickerProperties;
import com.seibel.cancer.database.db.service.StagingRawTrialDbService;
import com.seibel.cancer.database.db.service.TrialSourceDbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches studies from ClinicalTrials.gov and writes them to the staging table.
 * Normalization into the core schema is a separate step (TrialNormalizationService).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClinicalTrialsGovIngestJob {

    public static final String TRIAL_SOURCE_CODE = "CLINICALTRIALS_GOV";

    private final ClinicalTrialsGovClient client;
    private final TrialSourceDbService trialSourceDbService;
    private final StagingRawTrialDbService stagingRawTrialDbService;
    private final ProgressTickerProperties progressProperties;

    public IngestResult run(String condition, String term, String location, int maxStudies) {
        return run(condition, term, location, null, maxStudies);
    }

    /**
     * @param overallStatus CT.gov {@code filter.overallStatus} (e.g. RECRUITING). Blank for all
     *                      statuses.
     */
    public IngestResult run(String condition, String term, String location, String overallStatus,
                            int maxStudies) {
        TrialSource trialSource = trialSourceDbService.findByCode(TRIAL_SOURCE_CODE);
        if (trialSource == null) {
            throw new IllegalStateException(
                    "TrialSource '" + TRIAL_SOURCE_CODE + "' not seeded - check 100-load-init-data.yaml");
        }

        List<JsonNode> studies = client.searchStudies(condition, term, location, overallStatus, maxStudies);
        LocalDateTime fetchedAt = LocalDateTime.now();

        int stagedCount = 0;
        int skippedCount = 0;
        List<String> errors = new ArrayList<>();

        // Errors are logged at debug rather than error here: a log line fired mid-loop lands in
        // the middle of the progress bar and shreds it. Nothing is lost - every failure still
        // goes into the errors list returned below and counted in the summary line.
        try (ProgressTicker ticker = new ProgressTicker("STAGING",
                progressProperties.getLineWidth(),
                progressProperties.getFlushInterval(),
                progressProperties.resolveEnabled(System.console() != null),
                studies.size())) {
            for (JsonNode study : studies) {
                try {
                    String nctId = client.extractNctId(study);
                    if (nctId == null) {
                        errors.add("Study missing nctId, skipped");
                        ticker.error();
                        continue;
                    }
                    String rawJson = client.toRawJson(study);

                    StagingRawTrial existing = stagingRawTrialDbService
                            .findByTrialSourceIdAndSourceTrialId(trialSource.getId(), nctId);

                    if (existing == null) {
                        stagingRawTrialDbService.create(trialSource.getId(), nctId, rawJson, fetchedAt, null, null);
                        stagedCount++;
                        ticker.tick();
                    } else if (existing.getNormalizedAt() == null) {
                        // Already staged and still pending normalization - avoid a duplicate row.
                        skippedCount++;
                        ticker.skip();
                    } else {
                        // Already normalized before - refresh with the latest payload and re-queue it.
                        stagingRawTrialDbService.refreshForRenormalization(existing.getExtid(), rawJson, fetchedAt);
                        stagedCount++;
                        ticker.tick();
                    }
                } catch (Exception e) {
                    log.debug("Failed to stage study", e);
                    errors.add("Failed to stage study: " + e.getMessage());
                    ticker.error();
                }
            }
        }

        log.info("run(): fetched={}, staged={}, skipped={}, errors={}",
                studies.size(), stagedCount, skippedCount, errors.size());
        return new IngestResult(studies.size(), stagedCount, skippedCount, errors);
    }

    public record IngestResult(int studiesFetched, int stagingRowsWritten, int stagingRowsSkipped, List<String> errors) {
    }
}
