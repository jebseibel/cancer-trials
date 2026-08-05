package com.seibel.cancer.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestLabResultComponentCreate extends BaseRequest {

    /** extid of the parent lab result - never its numeric id (extid-only API rule). */
    @NotEmpty(message = "The labResultExtid is required.")
    @Size(max = 36, message = "The labResultExtid must be at most 36 characters.")
    private String labResultExtid;

    @NotEmpty(message = "The componentName is required.")
    @Size(max = 500, message = "The componentName must be at most 500 characters.")
    private String componentName;

    @Size(max = 32, message = "The loincCode must be at most 32 characters.")
    private String loincCode;

    private BigDecimal valueQuantity;

    @Size(max = 64, message = "The valueUnit must be at most 64 characters.")
    private String valueUnit;

    @Size(max = 1000, message = "The valueString must be at most 1000 characters.")
    private String valueString;

    @Size(max = 128, message = "The interpretation must be at most 128 characters.")
    private String interpretation;

    private BigDecimal referenceRangeLow;

    private BigDecimal referenceRangeHigh;

    @Size(max = 255, message = "The referenceRangeText must be at most 255 characters.")
    private String referenceRangeText;

    @NotEmpty(message = "The displayText is required.")
    @Size(max = 1000, message = "The displayText must be at most 1000 characters.")
    private String displayText;
}
