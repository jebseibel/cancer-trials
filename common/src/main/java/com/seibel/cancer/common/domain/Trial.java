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
}
