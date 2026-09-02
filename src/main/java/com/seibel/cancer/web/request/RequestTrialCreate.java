package com.seibel.cancer.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestTrialCreate extends BaseRequest {

    @Size(max = 16, message = "The nctId must be at most 16 characters.")
    private String nctId;

    @NotEmpty(message = "The briefTitle is required.")
    @Size(max = 500, message = "The briefTitle must be at most 500 characters.")
    private String briefTitle;

    @Size(max = 1000, message = "The officialTitle must be at most 1000 characters.")
    private String officialTitle;

    @Size(max = 500, message = "The friendlyTitle must be at most 500 characters.")
    private String friendlyTitle;

    @Size(max = 32, message = "The overallStatus must be at most 32 characters.")
    private String overallStatus;

    @Size(max = 32, message = "The studyType must be at most 32 characters.")
    private String studyType;

    private String briefSummary;

    private String detailedDescription;

    private LocalDate startDate;

    private LocalDate primaryCompletionDate;

    private LocalDate completionDate;

    private LocalDate lastUpdatePostedDate;

    private Integer enrollmentCount;

    @Size(max = 16, message = "The enrollmentType must be at most 16 characters.")
    private String enrollmentType;

    private Boolean healthyVolunteers;

    @Size(max = 8, message = "The sex must be at most 8 characters.")
    private String sex;

    @Size(max = 32, message = "The minimumAge must be at most 32 characters.")
    private String minimumAge;

    @Size(max = 32, message = "The maximumAge must be at most 32 characters.")
    private String maximumAge;

    private String eligibilityCriteria;

    private Boolean isPaidStudy;

    private BigDecimal paidAmount;

    @NotNull(message = "The primaryTrialSourceId is required.")
    private Long primaryTrialSourceId;
}
