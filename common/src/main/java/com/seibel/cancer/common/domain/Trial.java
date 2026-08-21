package com.seibel.cancer.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Trial extends BaseDomain {
    private String nctId;
    private String briefTitle;
    private String officialTitle;
    private String overallStatus;
    private String studyType;

    /**
     * What the trial appears to be trying to achieve, as
     * {@link com.seibel.cancer.common.enums.TreatmentGoal}'s name.
     *
     * <p>Derived from the title and summary at normalization, because CT.gov publishes no
     * treatment-intent field. A cached inference: it goes stale when the patterns change.
     */
    private String treatmentGoal;

    /**
     * What stage of disease the trial studies, as
     * {@link com.seibel.cancer.common.enums.DiseaseStage}'s name.
     *
     * <p>A third of the corpus is early-stage and therefore a mismatch for a metastatic
     * patient. Derived from prose at normalization; a cached inference like treatment goal.
     */
    private String diseaseStage;
    private String briefSummary;
    private String detailedDescription;
    private LocalDate startDate;
    private LocalDate primaryCompletionDate;
    private LocalDate completionDate;
    private LocalDate lastUpdatePostedDate;
    private Integer enrollmentCount;
    private String enrollmentType;
    private Boolean healthyVolunteers;
    private String sex;
    private String minimumAge;
    private String maximumAge;
    private String eligibilityCriteria;
    private Boolean isPaidStudy;
    private BigDecimal paidAmount;
    private Long primaryTrialSourceId;
}
