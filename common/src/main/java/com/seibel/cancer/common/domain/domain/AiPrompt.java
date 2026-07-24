package com.seibel.cancer.common.domain.domain;

import com.seibel.cancer.common.domain.BaseDomain;

import com.seibel.cancer.common.domain.enums.ai.AiLifecycle;
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
public class AiPrompt extends BaseDomain {
    private String uniqueId;
    private String name;
    private String description;
    private String trackingSystem;
    private String provider;
    private String model;
    private String version;
    private String content;
    private String jsonVariables;
    private String jsonVariablesTest;
    private String resultFormat;
    private String exampleInput;
    private String exampleOutput;
    private String envelopeExtid;
    private String envelopeName;
    private AiLifecycle lifecycle;
}
