package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * What one treatment-goal backfill run did.
 *
 * <p>{@code unchanged} is reported separately from {@code updated} on purpose. Most runs follow
 * a pattern edit that moves a handful of trials, so the interesting number is how many actually
 * changed — collapsing the two would hide whether an edit did anything at all.
 */
@Data
@Builder
public class ResponseTreatmentGoalBackfill {

    /** Active trials examined. */
    private int trialsRead;

    /** Trials whose stored goal differed from the freshly derived one and was rewritten. */
    private int updated;

    /** Trials already carrying the right value. Nothing to do is not the same as nothing read. */
    private int unchanged;

    /** Per-trial failures. A malformed row is recorded and the run continues. */
    private List<String> errors;
}
