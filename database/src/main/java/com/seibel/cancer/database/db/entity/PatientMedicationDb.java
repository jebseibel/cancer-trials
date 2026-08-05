package com.seibel.cancer.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "patient_medication")
public class PatientMedicationDb extends BaseDb {

    private static final long serialVersionUID = 1234567890123456820L;

    @Column(name = "fhir_resource_id", length = 64, nullable = false, unique = true)
    private String fhirResourceId;

    @Column(name = "medication_name", length = 500, nullable = false)
    private String medicationName;

    @Column(name = "rxnorm_code", length = 32)
    private String rxnormCode;

    @Column(name = "status", length = 32)
    private String status;

    @Column(name = "intent", length = 32)
    private String intent;

    @Column(name = "authored_on")
    private LocalDate authoredOn;

    @Column(name = "dosage_text", length = 1000)
    private String dosageText;

    @Column(name = "dose_quantity", precision = 12, scale = 3)
    private BigDecimal doseQuantity;

    @Column(name = "dose_unit", length = 64)
    private String doseUnit;

    @Column(name = "route", length = 128)
    private String route;

    @Column(name = "frequency_text", length = 255)
    private String frequencyText;

    @Column(name = "prescriber_name", length = 255)
    private String prescriberName;

    @Column(name = "reason_text", length = 1000)
    private String reasonText;

    @Column(name = "validity_start")
    private LocalDate validityStart;

    @Column(name = "validity_end")
    private LocalDate validityEnd;

    @Column(name = "refills_allowed")
    private Integer refillsAllowed;

    @Column(name = "display_text", columnDefinition = "text", nullable = false)
    private String displayText;
}
