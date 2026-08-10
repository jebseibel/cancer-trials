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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
        // Subset of skippedCount: skipped because the payload was byte-identical to what is
        // already normalized. Logged separately so a re-pull's saving is visible.
        int unchangedCount = 0;
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
                    // Hashed from the exact string that gets stored, not from a second
                    // serialization of the node - two serializations can differ in whitespace or
                    // key order and would produce a hash that does not describe its own payload.
                    String payloadHash = sha256Hex(rawJson);

                    StagingRawTrial existing = stagingRawTrialDbService
                            .findByTrialSourceIdAndSourceTrialId(trialSource.getId(), nctId);

                    if (existing == null) {
                        stagingRawTrialDbService.create(trialSource.getId(), nctId, rawJson, payloadHash,
                                fetchedAt, null, null);
                        stagedCount++;
                        ticker.tick();
                    } else if (existing.getNormalizedAt() == null) {
                        // Already staged and still pending normalization - avoid a duplicate row.
                        skippedCount++;
                        ticker.skip();
                    } else if (payloadHash != null && payloadHash.equals(existing.getPayloadHash())) {
                        // Normalized already and the payload has not changed since. Leave
                        // normalizedAt alone so the row stays out of the pending queue -
                        // re-normalizing is ~349ms and 99% of a run's cost, for no new data.
                        // A null stored hash falls through to the refresh below on purpose:
                        // unknown must mean refresh, never skip.
                        unchangedCount++;
                        skippedCount++;
                        ticker.skip();
                    } else {
                        // Already normalized before - refresh with the latest payload and re-queue it.
                        stagingRawTrialDbService.refreshForRenormalization(existing.getExtid(), rawJson,
                                payloadHash, fetchedAt);
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

        log.info("run(): fetched={}, staged={}, skipped={} (unchanged={}), errors={}",
                studies.size(), stagedCount, skippedCount, unchangedCount, errors.size());
        return new IngestResult(studies.size(), stagedCount, skippedCount, unchangedCount, errors);
    }

    /**
     * SHA-256 of the payload as lowercase hex. Returns null if the digest is unavailable, which
     * makes the caller fall back to refreshing - the safe direction, since an unknown hash must
     * never be read as "unchanged".
     */
    private String sha256Hex(String payload) {
        if (payload == null) {
            return null;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            log.debug("SHA-256 unavailable, falling back to unconditional refresh", e);
            return null;
        }
    }

    /**
     * @param stagingRowsUnchanged subset of stagingRowsSkipped: already normalized and the
     *                             payload hashed identically, so normalization was skipped
     *                             entirely. This is the saving a re-pull produces.
     */
    public record IngestResult(int studiesFetched, int stagingRowsWritten, int stagingRowsSkipped,
                               int stagingRowsUnchanged, List<String> errors) {
    }
}
