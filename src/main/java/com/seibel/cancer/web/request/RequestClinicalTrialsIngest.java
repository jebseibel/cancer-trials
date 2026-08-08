package com.seibel.cancer.web.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Ingestion request. Every field is optional - omitting one falls back to the configured
 * default under {@code cancer.ingestion.clinicaltrials.*} in application.yml.
 *
 * <p>Fields are boxed rather than primitive for exactly that reason: a primitive with a field
 * initializer cannot be distinguished from a caller explicitly sending that value, which would
 * make the configured default unreachable.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RequestClinicalTrialsIngest extends BaseRequest {

    /** Maps to ClinicalTrials.gov's query.cond. Defaults to cancer.ingestion.clinicaltrials.condition. */
    private String condition;

    /** Maps to query.term - free-text search term. */
    private String term;

    /** Maps to query.locn - location search term. */
    private String location;

    /**
     * Maps to filter.overallStatus - e.g. RECRUITING, ACTIVE_NOT_RECRUITING, COMPLETED.
     * Defaults to cancer.ingestion.clinicaltrials.overall-status (RECRUITING).
     * Send {@code "ALL"} to explicitly clear the filter and pull every status.
     */
    private String overallStatus;

    /**
     * Cap on studies fetched. Defaults to cancer.ingestion.clinicaltrials.max-studies (1000).
     *
     * <p>The 50,000 ceiling is a guardrail against a typo triggering an enormous pull - "cancer"
     * with no status filter matches 122,393 studies. For scale: cancer + RECRUITING is 18,773,
     * breast cancer + RECRUITING is 2,456.
     */
    @Min(value = 1, message = "The maxStudies must be at least 1.")
    @Max(value = 50000, message = "The maxStudies must be at most 50000.")
    private Integer maxStudies;
}
