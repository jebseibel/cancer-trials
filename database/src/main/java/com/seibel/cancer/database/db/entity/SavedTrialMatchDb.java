package com.seibel.cancer.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "trial_match")
public class SavedTrialMatchDb extends BaseDb {

    private static final long serialVersionUID = 1234567890123456824L;

    @Column(name = "trial_id", nullable = false)
    private Long trialId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    /** Nullable on purpose: a match must survive the diagnosis row being deleted and recreated. */
    @Column(name = "patient_diagnosis_id")
    private Long patientDiagnosisId;

    @Column(name = "search_run_id", length = 36, nullable = false)
    private String searchRunId;

    @Column(name = "query_text", columnDefinition = "text", nullable = false)
    private String queryText;

    @Column(name = "top_score", precision = 6, scale = 4, nullable = false)
    private BigDecimal topScore;

    @Column(name = "match_rank")
    private Integer matchRank;

    @Column(name = "snapshot_er_status", length = 16)
    private String snapshotErStatus;

    @Column(name = "snapshot_pr_status", length = 16)
    private String snapshotPrStatus;

    @Column(name = "snapshot_her2_status", length = 16)
    private String snapshotHer2Status;

    @Column(name = "snapshot_stage", length = 16)
    private String snapshotStage;

    @Column(name = "snapshot_biomarkers", length = 1000)
    private String snapshotBiomarkers;

    @Column(name = "matched_at", nullable = false)
    private LocalDateTime matchedAt;
}
