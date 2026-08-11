package com.seibel.cancer.common.enums;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Whether a genomic variant was found, in the five states a real report can express.
 *
 * <p><b>Not a boolean, and the distinction is clinical rather than pedantic.</b>
 * {@link #NOT_TESTED} and {@link #NOT_DETECTED} both mean "no variant to act on today", but
 * they answer different questions: an untested gene is an open question a trial coordinator
 * would ask about, while a tested-and-negative gene is a genuine mismatch for a trial that
 * requires the mutation. Collapsing them to {@code false} loses exactly the information that
 * decides whether a trial is worth a phone call.
 *
 * <p>Concretely: this patient's germline panel came back negative for pathogenic variants, so
 * BRCA1, BRCA2, PALB2, ATM and CHEK2 are {@code NOT_DETECTED}. That moves PARP-inhibitor
 * trials from "open question" to "genuine mismatch" — which is only expressible because the
 * vocabulary has both states.
 *
 * <p>Stored as a plain {@code varchar} rather than an enum column, so a value read from the
 * database may not match any constant here. Use {@link #fromValue(String)}, which maps
 * anything unrecognised to {@link #UNKNOWN} rather than throwing — a surprising value in one
 * field must not take down a match run.
 *
 * @see PatientVariant
 */
public enum VariantStatus {

    /** The variant is present. A trial requiring it is a potential fit. */
    DETECTED,

    /** Tested for, and absent. A trial requiring the variant is a genuine mismatch. */
    NOT_DETECTED,

    /**
     * Variant of uncertain significance: found, but its clinical meaning is unestablished.
     * Deliberately neither detected nor not-detected — treat as a question, never as a fit.
     */
    VUS,

    /** Never tested. An open question, not a negative result. */
    NOT_TESTED,

    /** Tested state genuinely unknown, or a value that does not map to this vocabulary. */
    UNKNOWN;

    public static final String ALLOWED_VALUES =
            Arrays.stream(values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));

    /**
     * Maps a stored string to a constant, tolerating case and surrounding whitespace.
     *
     * @param value raw column value; null, blank, or unrecognised all yield {@link #UNKNOWN}
     */
    public static VariantStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        String normalized = value.trim().toUpperCase();
        for (VariantStatus status : values()) {
            if (status.name().equals(normalized)) {
                return status;
            }
        }
        return UNKNOWN;
    }

    /** True when the variant is present and actionable — {@code DETECTED} only. */
    public boolean isDetected() {
        return this == DETECTED;
    }

    /**
     * True when a trial requiring this variant is a genuine mismatch rather than an open
     * question. Only {@code NOT_DETECTED} qualifies: an untested gene is unanswered, and a VUS
     * is unresolved.
     */
    public boolean isRuledOut() {
        return this == NOT_DETECTED;
    }

    /**
     * True when the answer is not established — {@code NOT_TESTED}, {@code VUS} or
     * {@code UNKNOWN}. These must surface as "ask about this", never as a pass or a fail.
     */
    public boolean isUnresolved() {
        return this == NOT_TESTED || this == VUS || this == UNKNOWN;
    }
}
