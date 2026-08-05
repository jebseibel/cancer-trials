package com.seibel.cancer.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LabResultComponent extends BaseDomain {
    private Long labResultId;
    private String componentName;
    private String loincCode;
    private BigDecimal valueQuantity;
    private String valueUnit;
    private String valueString;
    private String interpretation;
    private BigDecimal referenceRangeLow;
    private BigDecimal referenceRangeHigh;
    private String referenceRangeText;
    private String displayText;
}
