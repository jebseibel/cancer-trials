package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ResponseEligibilityRule {
    private String extid;
    private Long trialId;
    private Long parentRuleId;
    private String nodeType;
    private String operator;
    private String criterionType;
    private Long criterionId;
    private String requirementType;
    private Integer sortOrder;
    private String notes;
}
