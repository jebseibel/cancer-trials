package com.seibel.cancer.common.domain.matching;

/**
 * The result of testing one eligibility signal against one trial.
 *
 * <p>Four states, not two, and the fourth is the point. Per the project's no-verdicts rule an
 * unanswerable signal is a first-class result: a criterion this tool could not parse must
 * surface as "ask about this", never as a silent pass or a silent fail. A parsing failure that
 * removed a trial would take an option away from the patient for a reason that has nothing to
 * do with her.
 *
 * <p>{@link #NOT_APPLICABLE} is separate from {@link #UNKNOWN} because they say different
 * things to a reader. A trial that never mentions HER2 has nothing to answer; a trial whose
 * HER2 requirement could not be parsed has something to answer that was not answered. Folding
 * them together would inflate the second into noise or hide it in the first.
 */
public enum SignalOutcome {

    /** The patient meets what the trial appears to require on this signal. */
    PASS,

    /**
     * The trial appears to require something the patient does not have. Demotes and flags -
     * never removes. Receptor status can be re-tested and treatment history can change.
     */
    CONCERN,

    /** The signal applies but could not be resolved from the criteria text. Renders amber. */
    UNKNOWN,

    /** The trial says nothing about this signal, so there is nothing to answer. */
    NOT_APPLICABLE
}
