package com.seibel.cancer.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PatientMedication extends BaseDomain {
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
