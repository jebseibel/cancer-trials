package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ResponseLabResultComponent {
    private String extid;
    /** extid of the parent lab result - never its numeric id (extid-only API rule). */
    private String labResultExtid;
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
