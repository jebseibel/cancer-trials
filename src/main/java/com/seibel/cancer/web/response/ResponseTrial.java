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

    /**
     * What the trial appears to be trying to achieve: ABLATIVE, CURE_LANGUAGE or NOT_STATED.
     *
     * <p>Read-only. It is derived from the trial's own text rather than supplied by a caller,
     * so there is no matching field on the create or update requests - a client that could set
     * it could disagree with the classifier and nothing would reconcile them.
     */
    private String treatmentGoal;

    /**
     * What stage of disease the trial studies: METASTATIC, EARLY_STAGE, BOTH or NOT_STATED.
     *
     * <p>Read-only, like treatment goal - derived from the trial's own text rather than
     * supplied by a caller.
     */
    private String diseaseStage;

    /**
     * Where the trial runs, as "City, State" for US sites or country names otherwise.
     *
     * <p>Populated on the list endpoint, where a reader is scanning and travel is often what
     * decides whether a trial is possible at all. Empty on the single-trial endpoint, which has
     * a full locations section of its own.
     */
    private java.util.List<String> siteLabels;

    /** Total recorded sites, so a truncated label list can say how many more there are. */
    private Integer siteCount;

    /** Whether any site is in the United States. Null when locations were not looked up. */
    private Boolean hasUnitedStatesSite;
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
