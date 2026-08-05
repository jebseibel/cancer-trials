package com.seibel.cancer.web.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestLabResultUpdate extends BaseRequest {

    @Size(max = 64, message = "The fhirResourceId must be at most 64 characters.")
    private String fhirResourceId;

    @Size(max = 500, message = "The testName must be at most 500 characters.")
    private String testName;

    @Size(max = 32, message = "The loincCode must be at most 32 characters.")
    private String loincCode;

    @Size(max = 32, message = "The status must be at most 32 characters.")
    private String status;

    @Size(max = 64, message = "The category must be at most 64 characters.")
    private String category;

    private LocalDateTime effectiveAt;

    private LocalDateTime issuedAt;

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

    private Boolean isPanel;

    private String displayText;
}
