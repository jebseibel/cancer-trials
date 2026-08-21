package com.seibel.cancer.common.enums;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * What stage of disease a trial appears to be studying.
 *
 * <p>The other half of the question {@link TreatmentGoal} answers. A trial attempting cure in
 * early-stage disease is a correct match on ambition and useless to someone already metastatic,
 * and the two must be able to disagree — a combined value reporting "no" would give no way to
 * tell whether a trial is the wrong stage or merely the wrong aim, and those lead to different
 * conversations with an oncologist.
 *
 * <p><b>A third of the corpus is the wrong stage.</b> Measured across all 2,473 trials on
 * 2026-08-21: 823 (33.3%) use early-stage vocabulary — adjuvant, neoadjuvant, operable, stage
 * I-III. For a metastatic patient that is a third of every list that cannot apply, and until
 * this existed nothing outside the ranked list could tell them apart.
 *
 * <p>Like treatment goal, ClinicalTrials.gov publishes nothing structured for this, so it is
 * inferred from the trial's own prose and will sometimes be wrong. Per the no-verdicts rule the
 * value demotes and flags but never removes a trial.
 *
 * <p>Stored as {@code varchar(24)}; use {@link #fromValue(String)}, which maps anything
 * unrecognised to {@link #NOT_STATED} rather than throwing.
 */
public enum DiseaseStage {

    /**
     * Cancer that has spread — metastatic, stage IV, recurrent, M1.
     *
     * <p>{@code metasta(tic|sis|ses|tases)} is matched as a stem rather than the adjective alone.
     * NCT03808337, the clearest curative-intent trial in the corpus, says "1-5 metastases", and
     * an adjective-only pattern dropped it entirely while the distribution looked healthy.
     */
    METASTATIC,

    /**
     * Disease before it has spread — adjuvant, neoadjuvant, operable, stage 0-III, DCIS.
     *
     * <p>A mismatch for a metastatic patient, and the largest single category of mismatch in the
     * corpus.
     */
    EARLY_STAGE,

    /**
     * The description names both, which is common and genuinely ambiguous.
     *
     * <p>A trial may enrol early-stage patients while discussing metastatic disease in its
     * rationale, or run separate cohorts. Saying so is more honest than picking a side, and it
     * is why this vocabulary has four values rather than three.
     */
    BOTH,

    /**
     * Nothing in the title or summary says what stage it studies.
     *
     * <p>Also the value where there was no text to read. Absence of evidence, recorded as such —
     * it must not read as evidence that a trial is the wrong stage.
     */
    NOT_STATED;

    public static final String ALLOWED_VALUES =
            Arrays.stream(values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));

    /**
     * Maps a stored string to a constant.
     *
     * @param value raw column value; null, blank, or unrecognised all yield {@link #NOT_STATED},
     *              because one odd value must not fail a whole ranking run
     */
    public static DiseaseStage fromValue(String value) {
        if (value == null || value.isBlank()) {
            return NOT_STATED;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NOT_STATED;
        }
    }

    /**
     * True when this trial could apply to someone with metastatic disease.
     *
     * <p>{@link #BOTH} and {@link #NOT_STATED} both count. Only an unambiguously early-stage
     * trial is excluded, because a filter built on an inference must fail towards showing a
     * trial rather than hiding one.
     */
    public boolean couldIncludeMetastatic() {
        return this != EARLY_STAGE;
    }
}
