package com.seibel.cancer.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * What a model reported after reading one trial's criteria against one patient's record.
 *
 * <p>Stored rather than left on the page because this reading, unlike the deterministic signals,
 * costs money and is not reproducible: re-running gives a slightly different answer, so what she
 * actually read is only recoverable if it was kept.
 *
 * <p><b>There is no eligibility field, deliberately.</b> {@code rulesPatientOut} false means
 * nothing in the criteria excluded her — not that she qualifies. Same rule the rest of the
 * application follows, enforced here by the shape rather than by convention.
 *
 * <p>The {@code snapshot*} fields duplicate diagnosis values as they were when the reading was
 * made, for the same reason {@link SavedTrialMatch} does: {@code patient_diagnosis} is a single
 * row updated in place with no version history, so a stored assessment with no record of what it
 * was reading is worse than no assessment.
 *
 * <p>{@code model} and {@code promptHash} exist so a changed answer can be attributed. Two runs
 * months apart may differ because the model or the instructions changed rather than because
 * anything clinical did.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AiTrialAssessment extends BaseDomain {

    private Long trialId;
    private Long patientId;

    /** True only when a criterion clearly excludes the patient. False means nothing did. */
    private Boolean rulesPatientOut;

    /** The excluding criterion, quoted verbatim so a reader can verify it. */
    private String exclusionCriterion;

    private String summary;

    /** JSON arrays. Prose for a human to read, never queried on - see changeset 033. */
    private String openQuestions;
    private String concerns;
    private String criteriaMet;

    private String model;
    private String promptHash;

    private String snapshotStage;
    private String snapshotErStatus;
    private String snapshotPrStatus;
    private String snapshotHer2Status;

    private LocalDateTime assessedAt;
}
