package com.seibel.cancer.common.enums;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * A patient's history with a drug class, in the five states that change trial eligibility.
 *
 * <p><b>This is the vocabulary a boolean gets wrong, and the failure is concrete.</b> This
 * patient case is on a CDK4/6 inhibitor now and has not progressed. A boolean {@code priorCdk46 =
 * true} is literally accurate and reads as post-CDK4/6 — which matches that patient to trials designed
 * for people whose CDK4/6 inhibitor stopped working, the wrong half of the corpus. One
 * dropdown is the difference between a useful shortlist and a misleading one.
 *
 * <p>The three states trials actually distinguish:
 * <ul>
 *   <li>{@link #NEVER} — eligible for treatment-naive and first-line trials.</li>
 *   <li>{@link #CURRENT} — on it now, still working. Neither naive nor progressed.</li>
 *   <li>{@link #PROGRESSED} — it stopped working. This is what "post-CDK4/6" trials mean.</li>
 * </ul>
 *
 * <p>{@link #STOPPED_OTHER} exists because stopping for toxicity, cost, or choice is not
 * progression, and conflating the two would claim a resistance the disease has not shown.
 *
 * <p>Stored as a plain {@code varchar}, so use {@link #fromValue(String)} — it maps anything
 * unrecognised to {@link #UNKNOWN} rather than throwing.
 *
 * @see PatientPriorTreatment
 */
public enum TreatmentStatus {

    /** Never received this drug class. Eligible for treatment-naive and first-line trials. */
    NEVER,

    /**
     * Receiving it now, without documented progression. Not treatment-naive, and explicitly
     * not post-progression — the distinction a boolean destroys.
     */
    CURRENT,

    /** Received it and the disease progressed on it. What "post-<class>" trials require. */
    PROGRESSED,

    /**
     * Stopped for a reason other than progression — toxicity, cost, or patient choice.
     * Deliberately distinct from {@link #PROGRESSED}: the drug was not shown to fail.
     */
    STOPPED_OTHER,

    /** History genuinely unknown, or a value that does not map to this vocabulary. */
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
    public static TreatmentStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        String normalized = value.trim().toUpperCase();
        for (TreatmentStatus status : values()) {
            if (status.name().equals(normalized)) {
                return status;
            }
        }
        return UNKNOWN;
    }

    /** True only for {@link #NEVER} — the state a treatment-naive trial requires. */
    public boolean isNaive() {
        return this == NEVER;
    }

    /**
     * True only for {@link #PROGRESSED} — the state a post-progression trial requires.
     * {@link #CURRENT} and {@link #STOPPED_OTHER} are deliberately excluded: neither has
     * demonstrated resistance to this class.
     */
    public boolean hasProgressedOn() {
        return this == PROGRESSED;
    }

    /**
     * True when the class has been received at all, whatever the outcome. Useful for
     * "prior exposure" criteria, which several trials phrase without reference to progression.
     */
    public boolean hasReceived() {
        return this == CURRENT || this == PROGRESSED || this == STOPPED_OTHER;
    }

    /** True when the answer is not established. Must surface as "ask about this". */
    public boolean isUnresolved() {
        return this == UNKNOWN;
    }
}
