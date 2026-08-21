package com.seibel.cancer.common.enums;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * What a trial appears to be trying to achieve, as opposed to who it will enrol.
 *
 * <p>Every other signal in this project answers "does she qualify". This vocabulary answers the
 * other half — "and if she got in, what would this trial be trying to do for her" — which is the
 * question the tool was built for:
 *
 * <blockquote>Trials that are trying to cure stage 4 cancer. They are out there but they are few.
 * They are the primary ones I am trying to find.</blockquote>
 *
 * <p><b>Three values rather than a boolean, and the middle one is the point.</b> Measured across
 * all 2,473 trials on 2026-08-21: {@link #ABLATIVE} carried 26 of the 38 survivors at
 * near-perfect precision, while the 12 arriving on cure language alone contained every false
 * positive. Collapsing those two into "true" would discard exactly the confidence gradient the
 * measurement established, the same way a boolean receptor status would discard "not tested".
 *
 * <p><b>This is inferred from prose, and it will sometimes be wrong.</b> ClinicalTrials.gov
 * publishes no treatment-intent field, so there is nothing authoritative to read. Per the
 * project's no-verdicts rule the value demotes and flags but never removes a trial, and the
 * evidence phrase travels with it so a reader can check the reasoning rather than trust it.
 *
 * <p>Stored as {@code varchar(24)}; use {@link #fromValue(String)}, which maps anything
 * unrecognised to {@link #NOT_STATED} rather than throwing.
 */
public enum TreatmentGoal {

    /**
     * The trial treats the individual sites of spread — SBRT, oligometastatic strategy,
     * metastasis-directed therapy, ablation, metastasectomy.
     *
     * <p>The clinically real route by which a stage IV patient is treated with curative intent at
     * all, and the most precise signal available. This language names something being done to a
     * metastasis, so unlike response vocabulary it cannot appear in an endpoint definition or a
     * description of a patient's treatment history.
     */
    ABLATIVE,

    /**
     * The description uses the language of cure or long-term remission, without describing an
     * ablative strategy.
     *
     * <p>A question rather than an answer. The phrase is as likely to be background about the
     * disease — "metastatic breast cancer remains difficult to cure" — as a statement of this
     * study's aim, so it is worth surfacing above silence and below {@link #ABLATIVE}, with the
     * quoted text attached for the reader to judge.
     */
    CURE_LANGUAGE,

    /**
     * Nothing in the title or summary speaks to treatment goal.
     *
     * <p>Not a criticism of the trial. The overwhelming majority of metastatic breast cancer
     * trials test disease control, which is legitimate and is simply not what is being looked
     * for here. Also the value for a trial whose text could not be read at all — absence of
     * evidence, recorded as such.
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
    public static TreatmentGoal fromValue(String value) {
        if (value == null || value.isBlank()) {
            return NOT_STATED;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NOT_STATED;
        }
    }

    /** True when the trial appears to be aiming beyond disease control. */
    public boolean isCurativeIntent() {
        return this == ABLATIVE || this == CURE_LANGUAGE;
    }

    /**
     * Ranking order: lower sorts first.
     *
     * <p>Kept beside the values so the ordering cannot drift from the vocabulary if a value is
     * ever added.
     */
    public int rank() {
        return switch (this) {
            case ABLATIVE -> 0;
            case CURE_LANGUAGE -> 1;
            case NOT_STATED -> 2;
        };
    }
}
