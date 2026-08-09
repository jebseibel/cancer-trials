package com.seibel.cancer.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * Prior and current treatment exposure for one patient - one row per patient.
 *
 * <p>Distinct from {@link PatientMedication}, which mirrors a FHIR MedicationRequest (what
 * Epic prescribed, keyed on a required fhirResourceId). This records <em>exposure and
 * outcome</em>, which is what trial eligibility gates on, and is hand-enterable.
 *
 * <p>Every drug-class field uses the same five-state vocabulary:
 * {@code NEVER | CURRENT | PROGRESSED | STOPPED_OTHER | UNKNOWN}. A boolean would not do:
 * trials split into treatment-naive and post-progression populations, so "has taken a CDK4/6
 * inhibitor" is true of both a first-line patient currently on one and a patient who
 * progressed off one - and they qualify for opposite cohorts.
 *
 * <p>Design and rationale in {@code .claude/diagnosis/patient-variant-and-treatment-tables.md}.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PatientPriorTreatment extends BaseDomain {
    private Long appUserId;
    private Long patientDiagnosisId;

    // Priority 1 drug classes - the gates that appear most across this corpus
    private String cdk46Status;
    private String endocrineStatus;
    private String serdStatus;
    private String chemoStatus;
    private String her2TherapyStatus;
    private String her2AdcStatus;
    private String trop2AdcStatus;
    private String parpStatus;
    private String pi3kAktMtorStatus;
    private String immunotherapyStatus;

    // Chemotherapy sub-exposures - trials say "prior taxane", not "prior chemotherapy"
    private String taxaneStatus;
    private String anthracyclineStatus;
    private String platinumStatus;

    // Named drugs, where the specific agent matters
    private String currentDrugNames;
    private String priorDrugNames;

    // Line and setting
    /** 0 means treatment-naive in the metastatic setting - a meaningful value, not missing. */
    private Integer linesOfTherapyMetastatic;
    private Boolean hadNeoadjuvant;
    private Boolean hadAdjuvant;
    private Boolean hadRadiation;
    private Boolean hadSurgery;
    private LocalDate lastTreatmentEndDate;
    private Boolean currentlyOnTreatment;

    private String otherTreatments;
    private String notes;
}
