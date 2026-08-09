package com.seibel.cancer.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One trial surfaced by one diagnosis-driven search run. A run returning 10 trials writes
 * 10 rows sharing a searchRunId.
 *
 * The snapshot* fields deliberately duplicate diagnosis values as they were at match time.
 * patient_diagnosis is a single row updated in place with no version history, so a match
 * cannot reference "the diagnosis as it was" - the snapshot is what keeps a stored match
 * interpretable later. See .claude/diagnosis/trial-match-tables.md.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SavedTrialMatch extends BaseDomain {
    private Long trialId;
    private Long appUserId;
    private Long patientDiagnosisId;
    private String searchRunId;
    private String queryText;
    private BigDecimal topScore;
    private Integer matchRank;
    private String snapshotErStatus;
    private String snapshotPrStatus;
    private String snapshotHer2Status;
    private String snapshotStage;
    private String snapshotBiomarkers;
    private LocalDateTime matchedAt;
}
