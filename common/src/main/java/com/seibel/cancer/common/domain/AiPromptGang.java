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
public class AiPromptGang extends BaseDomain {
    private String uniqueId;
    private String name;
    private String description;
    private String persona;
    private String gang;
    private String subgang;
    private String tone;
    private String promptSequence;
    private String prohibitions;
    private String decisionPrinciples;
    private Integer memoryDepth;
    private String version;
    private String soulExtid;
    private AiLifecycle lifecycle;
}
