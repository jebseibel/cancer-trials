package com.seibel.cancer.common.domain.matching;

import java.util.List;

/**
 * Every assessed signal for one trial, plus the ordering information the caller needs.
 *
 * <p><b>There is deliberately no fit score or percentage here.</b> The previous attempt scored
 * {@code signals_matched / 6}, which was unreachable by construction — a trial cannot be both
 * "first-line treatment-naive" and "post-CDK4/6 progression" — and worse, it counted keyword
 * co-occurrence in criteria text rather than whether the patient qualifies. A number that looks
 * like a probability but is not one invites exactly the reliance this tool must not encourage.
 *
 * <p>What is exposed instead is countable and honest: how many signals raised a concern, how
 * many could not be resolved, and how many applied at all. A caller can order by those without
 * anyone reading a "73% match" that means nothing.
 *
 * @param trialExtid the trial this assesses
 * @param nctId      convenience copy, for display and logging
 * @param signals    every signal, including NOT_APPLICABLE ones, in a stable order
 */
public record TrialAssessment(String trialExtid, String nctId, List<EligibilitySignal> signals,
                              List<String> siteCities, int siteCount, boolean hasUnitedStatesSite) {

    public TrialAssessment {
        signals = signals == null ? List.of() : List.copyOf(signals);
        siteCities = siteCities == null ? List.of() : List.copyOf(siteCities);
    }

    /** Backwards-compatible constructor for callers with no location data to hand. */
    public TrialAssessment(String trialExtid, String nctId, List<EligibilitySignal> signals) {
        this(trialExtid, nctId, signals, List.of(), 0, false);
    }

    /** Signals that raised a concern. The primary demotion key - never a removal key. */
    public long concernCount() {
        return signals.stream().filter(s -> s.outcome() == SignalOutcome.CONCERN).count();
    }

    /** Signals that applied but could not be resolved. These are questions, not failures. */
    public long unknownCount() {
        return signals.stream().filter(s -> s.outcome() == SignalOutcome.UNKNOWN).count();
    }

    /** Signals the patient meets. */
    public long passCount() {
        return signals.stream().filter(s -> s.outcome() == SignalOutcome.PASS).count();
    }

    /**
     * Signals this trial actually spoke to.
     *
     * <p>The denominator worth reporting, when one is reported at all: a trial silent on three
     * of four signals has not passed three checks, it has answered one.
     */
    public long applicableCount() {
        return signals.stream().filter(EligibilitySignal::applies).count();
    }

    /** Only the signals worth a reader's attention - concerns first, then open questions. */
    public List<EligibilitySignal> notable() {
        return signals.stream()
                .filter(s -> s.outcome() == SignalOutcome.CONCERN || s.outcome() == SignalOutcome.UNKNOWN)
                .sorted((a, b) -> Boolean.compare(
                        b.outcome() == SignalOutcome.CONCERN,
                        a.outcome() == SignalOutcome.CONCERN))
                .toList();
    }
}
