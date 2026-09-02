package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * A model's reading of one trial's criteria against one patient's record.
 *
 * <p><b>There is deliberately no eligibility field and no score.</b> {@code rulesPatientOut} is
 * the only assertion the model is allowed to make, and it must be backed by a quoted criterion.
 * Its absence means "nothing here rules her out" — not that she qualifies. That judgement belongs
 * to the study team, and an AI stating it in fluent prose is the most believable form of the
 * verdict this application refuses to give everywhere else.
 */
@Data
@Builder
public class ResponseAiTrialCheck {

    /** True only when a criterion clearly excludes this patient. */
    private Boolean rulesPatientOut;

    /** The excluding criterion, quoted verbatim so a reader can verify it. Null otherwise. */
    private String exclusionCriterion;

    /** One sentence a non-clinician can read. */
    private String summary;

    /** Criteria the record appears to satisfy. Observations, not a finding of eligibility. */
    private List<String> criteriaSheAppearsToMeet;

    /** Criteria that cannot be judged from the record — the things worth asking the care team. */
    private List<String> openQuestions;

    /** Anything a reader should notice that the criteria alone do not say. */
    private List<String> concerns;

    /** Which model produced this, so an answer stays traceable to what generated it. */
    private String model;

    /** When this reading was made. Null on a fresh one - it is being made now. */
    private java.time.LocalDateTime assessedAt;
}
