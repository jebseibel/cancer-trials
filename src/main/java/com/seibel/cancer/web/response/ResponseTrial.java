package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ResponseTrial {
    private String extid;
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
