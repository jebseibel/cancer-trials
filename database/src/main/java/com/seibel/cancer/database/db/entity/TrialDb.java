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

    @Column(name = "overall_status", length = 32)
    private String overallStatus;

    @Column(name = "study_type", length = 32)
    private String studyType;

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
