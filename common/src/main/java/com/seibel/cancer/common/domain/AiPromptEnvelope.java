package com.seibel.cancer.common.domain;

import com.seibel.cancer.common.enums.ai.AiLifecycle;
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
public class AiPromptEnvelope extends BaseDomain {
    private String uniqueId;
    private String name;
    private String description;
    private String category;
    private String subCategory;
    private String resultType;
    private String subPersona;
    private String goal;
    private String decisionPrinciples;
    private String toolExclusions;
    private AiLifecycle lifecycle;
    private Double temperature;
    private Integer maxRetries;
    private Integer timeoutSeconds;
    private String version;
    private String gangExtid;
    private String gangName;
}
