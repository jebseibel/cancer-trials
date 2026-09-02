package com.seibel.cancer.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * ⚠️ Holds clinical prose about a real patient, derived from her record. Treat it with the same
 * care as {@code patient_diagnosis} - it is not a cache.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "ai_trial_assessment")
public class AiTrialAssessmentDb extends BaseDb {

    private static final long serialVersionUID = 1234567890123456833L;

    @Column(name = "trial_id", nullable = false)
    private Long trialId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "rules_patient_out")
    private Boolean rulesPatientOut;

    @Column(name = "exclusion_criterion", columnDefinition = "text")
    private String exclusionCriterion;

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    @Column(name = "open_questions", columnDefinition = "text")
    private String openQuestions;

    @Column(name = "concerns", columnDefinition = "text")
    private String concerns;

    @Column(name = "criteria_met", columnDefinition = "text")
    private String criteriaMet;

    @Column(name = "model", length = 64)
    private String model;

    @Column(name = "prompt_hash", length = 64)
    private String promptHash;

    @Column(name = "snapshot_stage", length = 64)
    private String snapshotStage;

    @Column(name = "snapshot_er_status", length = 32)
    private String snapshotErStatus;

    @Column(name = "snapshot_pr_status", length = 32)
    private String snapshotPrStatus;

    @Column(name = "snapshot_her2_status", length = 32)
    private String snapshotHer2Status;

    @Column(name = "assessed_at", nullable = false)
    private LocalDateTime assessedAt;
}
