package com.seibel.cancer.datafetcher.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Defaults for ClinicalTrials.gov ingestion, overridable per request from the frontend.
 *
 * <p>These are policy, not mechanism - what to pull when nobody says otherwise. The frontend
 * sends explicit values for a targeted pull; omitting a field falls back to the value here.
 *
 * <p><b>Scale worth knowing.</b> Live CT.gov counts as of 2026-08-07:
 * <ul>
 *   <li>{@code cancer} + RECRUITING - 18,773 trials (the default filter)</li>
 *   <li>{@code cancer}, any status - 122,393</li>
 *   <li>{@code breast cancer} + RECRUITING - 2,456; any status - 16,689</li>
 * </ul>
 * Staging those is cheap (JSON blob writes). <b>Embedding them is not</b> - at roughly 26
 * chunks per trial, 18,773 trials is ~490,000 local ONNX inferences. Fetch and embed are
 * separate steps ({@code POST /api/ingestion/clinicaltrials} then
 * {@code POST /api/rag/backfill}), so they can be run independently.
 *
 * <p>{@code maxStudies} defaults to 1,000 rather than unlimited so a routine run is a sensible
 * starting corpus and the full pull is a deliberate choice.
 */
@Data
@ConfigurationProperties(prefix = "cancer.ingestion.clinicaltrials")
public class ClinicalTrialsIngestProperties {

    /** Maps to CT.gov {@code query.cond}. */
    private String condition = "cancer";

    /**
     * Maps to CT.gov {@code filter.overallStatus}. RECRUITING by default: it is ~15% of the
     * corpus and the only part a patient can actually join, and the RAG evaluation showed a
     * corpus of mostly COMPLETED/TERMINATED trials is the wrong corpus for a discovery tool.
     * Blank means no status filter (all statuses).
     */
    private String overallStatus = "RECRUITING";

    /** Default cap per run. Raise per-request for a full pull. */
    private int maxStudies = 1000;

    /**
     * Upper bound on staging rows normalized per call.
     *
     * <p>Deliberately independent of {@code maxStudies}: staging rows accumulate across runs, so
     * tying the normalize limit to the fetch size silently leaves earlier rows pending.
     */
    private int maxNormalizeRows = 5000;
}
