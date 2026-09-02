package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * What one friendly-title backfill run did.
 *
 * <p>{@code alreadyPresent} is reported separately from {@code generated} on purpose. Each
 * generation is a paid AI call, so a run that generated nothing because a prior run already
 * covered the corpus is a very different outcome from a run that had nothing to read.
 */
@Data
@Builder
public class ResponseFriendlyTitleBackfill {

    /** Active trials examined. */
    private int trialsRead;

    /** Trials that had no friendly title and were sent to the model. */
    private int generated;

    /** Trials that already had a friendly title and were skipped, to avoid re-paying for them. */
    private int alreadyPresent;

    /** Per-trial failures. A malformed row is recorded and the run continues. */
    private List<String> errors;
}
