package com.seibel.cancer.service.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

import java.util.List;

/**
 * What a model reported after reading one trial's criteria against one patient record.
 *
 * <p><b>The field names and descriptions here are part of the prompt.</b> They are sent to the
 * model as a schema, so they read as instructions rather than as notes to a developer. Changing
 * a description changes the model's behaviour.
 *
 * <p><b>The shape enforces the project's no-verdicts rule.</b> There is deliberately no
 * {@code eligible} field and no score. The model can report that something <em>rules the patient
 * out</em>, which is a checkable claim backed by quoted text; it cannot report that she
 * qualifies, because that judgement belongs to her oncology team and an AI saying it in fluent
 * prose is the most believable form of the thing this tool refuses to say everywhere else.
 *
 * <p>Absence of an exclusion is therefore reported as absence — "nothing here rules her out" —
 * not as a match. Same asymmetry the deterministic signals already use: a concern is
 * informative, a pass is silence.
 *
 * <p><b>No field maps back onto a patient column.</b> This is read-only output about a trial; it
 * must never be able to overwrite clinician-entered diagnosis data.
 */
@Data
public class TrialMatchAssessment {

    @JsonPropertyDescription(
            "True only when a stated eligibility criterion clearly rules this patient out, based "
                    + "on what the record actually says. False when nothing in the criteria "
                    + "excludes her - which means only that, not that she qualifies. If you are "
                    + "unsure, set this false and add the doubt to openQuestions instead.")
    private Boolean rulesPatientOut;

    @JsonPropertyDescription(
            "When rulesPatientOut is true, the trial's own wording that excludes her, quoted "
                    + "verbatim so a reader can check it against the trial text. Null otherwise. "
                    + "Never paraphrase - the quote is the evidence.")
    private String exclusionCriterion;

    @JsonPropertyDescription(
            "One short sentence a non-clinician can read, saying what you compared and what you "
                    + "found. Do not state or imply that she is eligible.")
    private String summary;

    @JsonPropertyDescription(
            "Criteria this patient's record appears to satisfy, each quoted from the trial. "
                    + "These are observations, not a finding of eligibility. Empty list if none.")
    private List<String> criteriaSheAppearsToMeet;

    @JsonPropertyDescription(
            "Criteria that cannot be judged because the record does not say - missing lab "
                    + "values, untested biomarkers, unrecorded treatment history. Quote the "
                    + "criterion and name what is missing. This list is the most useful thing "
                    + "you produce: it is what she should ask her care team about.")
    private List<String> openQuestions;

    @JsonPropertyDescription(
            "Anything about this trial a reader should notice that the criteria alone do not "
                    + "say - a trial for an earlier stage of disease, a different cancer, or a "
                    + "design that would not suit her situation. Empty list if nothing.")
    private List<String> concerns;
}
