package com.seibel.cancer.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestEligibilityRuleCreate extends BaseRequest {

    @NotNull(message = "The trialId is required.")
    private Long trialId;

    private Long parentRuleId;

    @NotEmpty(message = "The nodeType is required.")
    @Size(max = 8, message = "The nodeType must be at most 8 characters.")
    private String nodeType;

    @Size(max = 8, message = "The operator must be at most 8 characters.")
    private String operator;

    @Size(max = 16, message = "The criterionType must be at most 16 characters.")
    private String criterionType;

    private Long criterionId;

    @Size(max = 16, message = "The requirementType must be at most 16 characters.")
    private String requirementType;

    @NotNull(message = "The sortOrder is required.")
    private Integer sortOrder;

    private String notes;
}
