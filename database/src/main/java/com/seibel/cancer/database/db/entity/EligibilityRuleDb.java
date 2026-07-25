package com.seibel.cancer.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "eligibility_rule")
public class EligibilityRuleDb extends BaseDb {

    private static final long serialVersionUID = 1234567890123456800L;

    @Column(name = "trial_id", nullable = false)
    private Long trialId;

    @Column(name = "parent_rule_id")
    private Long parentRuleId;

    @Column(name = "node_type", length = 8, nullable = false)
    private String nodeType;

    @Column(name = "operator", length = 8)
    private String operator;

    @Column(name = "criterion_type", length = 16)
    private String criterionType;

    @Column(name = "criterion_id")
    private Long criterionId;

    @Column(name = "requirement_type", length = 16)
    private String requirementType;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;
}
