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
public class Outcome extends BaseDomain {
    private Long trialId;
    private String outcomeType;
    private String measure;
    private String description;
    private String timeFrame;
}
