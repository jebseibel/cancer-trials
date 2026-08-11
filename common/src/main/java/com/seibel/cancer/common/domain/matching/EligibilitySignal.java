package com.seibel.cancer.common.domain.matching;

/**
 * One assessed eligibility signal, with the evidence that produced it.
 *
 * <p>{@code evidence} is the whole reason this is a record rather than a bare outcome. The
 * tool's job is to surface what to look into and ask about, so a reader has to be able to see
 * *why* a trial was flagged and judge the reasoning themselves. A flag with no quotable text
 * is an unexplained verdict, which is exactly what this project does not produce.
 *
 * @param name     short label shown to the reader, e.g. "HER2 status"
 * @param outcome  pass, concern, unknown, or not-applicable
 * @param detail   one sentence a non-clinician can read, stating what was compared
 * @param evidence the phrase from the trial's criteria that drove this, or null when nothing
 *                 in the text was matched (a NOT_APPLICABLE or UNKNOWN signal)
 */
public record EligibilitySignal(
        String name,
        SignalOutcome outcome,
        String detail,
        String evidence) {

    public static EligibilitySignal pass(String name, String detail, String evidence) {
        return new EligibilitySignal(name, SignalOutcome.PASS, detail, evidence);
    }

    public static EligibilitySignal concern(String name, String detail, String evidence) {
        return new EligibilitySignal(name, SignalOutcome.CONCERN, detail, evidence);
    }

    public static EligibilitySignal unknown(String name, String detail) {
        return new EligibilitySignal(name, SignalOutcome.UNKNOWN, detail, null);
    }

    public static EligibilitySignal notApplicable(String name) {
        return new EligibilitySignal(name, SignalOutcome.NOT_APPLICABLE,
                "This trial does not appear to specify anything about " + name.toLowerCase() + ".",
                null);
    }

    /** True when this signal was actually assessed - i.e. the trial had something to say. */
    public boolean applies() {
        return outcome != SignalOutcome.NOT_APPLICABLE;
    }
}
