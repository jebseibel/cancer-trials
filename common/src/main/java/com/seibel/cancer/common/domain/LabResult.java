package com.seibel.cancer.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LabResult extends BaseDomain {
    private String fhirResourceId;
    private String testName;
    private String loincCode;
    private String status;
    private String category;
    private LocalDateTime effectiveAt;
    private LocalDateTime issuedAt;
    private BigDecimal valueQuantity;
    private String valueUnit;
    private String valueString;
    private String interpretation;
    private BigDecimal referenceRangeLow;
    private BigDecimal referenceRangeHigh;
    private String referenceRangeText;
    private Boolean isPanel;
    private String displayText;
}
