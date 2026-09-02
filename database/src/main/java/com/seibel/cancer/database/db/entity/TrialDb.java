package com.seibel.cancer.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "trial")
public class TrialDb extends BaseDb {

    private static final long serialVersionUID = 1234567890123456790L;

    @Column(name = "nct_id", length = 16, unique = true)
    private String nctId;

    @Column(name = "brief_title", length = 500, nullable = false)
    private String briefTitle;

    @Column(name = "official_title", length = 1000)
    private String officialTitle;

    /**
     * A plain-language, non-technical version of {@link #briefTitle} for readers who are not
     * clinicians. ClinicalTrials.gov titles are written for a scientific audience.
     */
    @Column(name = "friendly_title", length = 500)
    private String friendlyTitle;

    @Column(name = "overall_status", length = 32)
    private String overallStatus;

    @Column(name = "study_type", length = 32)
    private String studyType;

    /**
     * Inferred treatment goal - see {@link com.seibel.cancer.common.enums.TreatmentGoal}.
     *
     * <p>Stored as a string rather than an enum type, matching how every other vocabulary in
     * this schema is persisted, so an unrecognised value maps to NOT_STATED instead of failing
     * to load the row.
     */
    @Column(name = "treatment_goal", length = 24)
    private String treatmentGoal;

    /**
     * Inferred disease stage - see {@link com.seibel.cancer.common.enums.DiseaseStage}.
     *
     * <p>Stored as a string for the same reason as treatment goal: an unrecognised value maps
     * to NOT_STATED rather than failing to load the row.
     */
    @Column(name = "disease_stage", length = 24)
    private String diseaseStage;

    @Column(name = "brief_summary", columnDefinition = "text")
    private String briefSummary;

    @Column(name = "detailed_description", columnDefinition = "text")
    private String detailedDescription;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "primary_completion_date")
    private LocalDate primaryCompletionDate;

    @Column(name = "completion_date")
    private LocalDate completionDate;

    @Column(name = "last_update_posted_date")
    private LocalDate lastUpdatePostedDate;

    @Column(name = "enrollment_count")
    private Integer enrollmentCount;

    @Column(name = "enrollment_type", length = 16)
    private String enrollmentType;

    @Column(name = "healthy_volunteers")
    private Boolean healthyVolunteers;

    @Column(name = "sex", length = 8)
    private String sex;

    @Column(name = "minimum_age", length = 32)
    private String minimumAge;

    @Column(name = "maximum_age", length = 32)
    private String maximumAge;

    @Column(name = "eligibility_criteria", columnDefinition = "text")
    private String eligibilityCriteria;

    @Column(name = "is_paid_study")
    private Boolean isPaidStudy;

    @Column(name = "paid_amount", precision = 10, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "primary_trial_source_id", nullable = false)
    private Long primaryTrialSourceId;
}
