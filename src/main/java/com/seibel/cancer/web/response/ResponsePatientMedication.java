package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ResponsePatientMedication {
    private String extid;
    private String fhirResourceId;
    private String medicationName;
    private String rxnormCode;
    private String status;
    private String intent;
    private LocalDate authoredOn;
    private String dosageText;
    private BigDecimal doseQuantity;
    private String doseUnit;
    private String route;
    private String frequencyText;
    private String prescriberName;
    private String reasonText;
    private LocalDate validityStart;
    private LocalDate validityEnd;
    private Integer refillsAllowed;
    private String displayText;
}
