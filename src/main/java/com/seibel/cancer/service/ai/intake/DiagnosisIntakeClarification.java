package com.seibel.cancer.service.ai.intake;

import lombok.Data;

/**
 * The response shape for one clarifying turn.
 *
 * <p>{@code updatedDraft} is the whole draft, re-emitted, not a patch - the model performs the
 * merge of the user's latest free-text answer into the existing draft, so there is no
 * hand-rolled Java merge/conflict logic to trust. A single answer can resolve more than the one
 * field that was asked about (for example, "triple negative" resolves ER, PR, and HER2 at once)
 * and a model reading the answer can catch that; a Java patch keyed to "which field was asked"
 * cannot, without reimplementing clinical language understanding.
 */
@Data
public class DiagnosisIntakeClarification {

    private DiagnosisIntakeExtraction updatedDraft;

    /** Null or blank when there is nothing further to ask. */
    private String nextQuestion;

    /** Explicit rather than inferred from a blank question, to avoid ambiguity. */
    private Boolean done;
}
