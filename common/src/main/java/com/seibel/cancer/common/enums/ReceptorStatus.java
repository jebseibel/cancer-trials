package com.seibel.cancer.common.enums;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * ER, PR or HER2 receptor status.
 *
 * <p>Receptor polarity gates roughly a third of this corpus — 36% on HER2, 28% on ER/PR — and
 * it is the axis embedding similarity cannot see. "HR-negative, HER2-negative" and
 * "HR-positive, HER2-negative" differ by one token inside an otherwise identical phrase, so a
 * triple-negative trial once scored highest for an ER-positive patient, ahead of the trial
 * that matched her line for line. No similarity threshold fixes that; a structured comparison
 * does, which is what this vocabulary is for.
 *
 * <p>{@link #UNKNOWN} is first-class. Per the project's no-verdicts rule an unknown receptor
 * status renders amber and never removes a trial — a value the tool could not establish must
 * not silently take an option away from the patient.
 *
 * <p>Stored as {@code varchar(16)}, so use {@link #fromValue(String)}, which tolerates the
 * spellings that actually appear in clinical text and maps anything else to {@link #UNKNOWN}.
 */
public enum ReceptorStatus {

    /** Receptor expressed. For ER or PR this makes the patient hormone-receptor-positive. */
    POSITIVE,

    /** Tested and not expressed. */
    NEGATIVE,

    /** Not established, or a value that does not map to this vocabulary. Renders amber. */
    UNKNOWN;

    public static final String ALLOWED_VALUES =
            Arrays.stream(values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));

    /**
     * Maps a stored string to a constant.
     *
     * <p>Accepts the {@code +}/{@code -} shorthand as well as the full words, because both
     * appear in real reports and in hand-entered data. Anything unrecognised becomes
     * {@link #UNKNOWN} rather than throwing — one odd value must not fail a match run.
     *
     * @param value raw column value; null, blank, or unrecognised all yield {@link #UNKNOWN}
     */
    public static ReceptorStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "POSITIVE", "POS", "+" -> POSITIVE;
            case "NEGATIVE", "NEG", "-" -> NEGATIVE;
            default -> UNKNOWN;
        };
    }

    public boolean isPositive() {
        return this == POSITIVE;
    }

    public boolean isNegative() {
        return this == NEGATIVE;
    }

    /** True when the status is not established. Must surface as a question, not a verdict. */
    public boolean isUnresolved() {
        return this == UNKNOWN;
    }
}
