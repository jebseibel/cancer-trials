package com.seibel.cancer.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EligibilityRule extends BaseDomain {
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
