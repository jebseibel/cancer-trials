package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ResponseLabResult {
    private String extid;
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
