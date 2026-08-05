package com.seibel.cancer.web.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestPatientMedicationUpdate extends BaseRequest {

    @Size(max = 64, message = "The fhirResourceId must be at most 64 characters.")
    private String fhirResourceId;

    @Size(max = 500, message = "The medicationName must be at most 500 characters.")
    private String medicationName;

    @Size(max = 32, message = "The rxnormCode must be at most 32 characters.")
    private String rxnormCode;

    @Size(max = 32, message = "The status must be at most 32 characters.")
    private String status;

    @Size(max = 32, message = "The intent must be at most 32 characters.")
    private String intent;

    private LocalDate authoredOn;

    @Size(max = 1000, message = "The dosageText must be at most 1000 characters.")
    private String dosageText;

    private BigDecimal doseQuantity;

    @Size(max = 64, message = "The doseUnit must be at most 64 characters.")
    private String doseUnit;

    @Size(max = 128, message = "The route must be at most 128 characters.")
    private String route;

    @Size(max = 255, message = "The frequencyText must be at most 255 characters.")
    private String frequencyText;

    @Size(max = 255, message = "The prescriberName must be at most 255 characters.")
    private String prescriberName;

    @Size(max = 1000, message = "The reasonText must be at most 1000 characters.")
    private String reasonText;

    private LocalDate validityStart;

    private LocalDate validityEnd;

    private Integer refillsAllowed;

    private String displayText;
}
