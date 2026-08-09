package com.seibel.cancer.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * Prior and current treatment exposure for one patient.
 *
 * <p>Drug-class columns use {@code NEVER | CURRENT | PROGRESSED | STOPPED_OTHER | UNKNOWN}
 * rather than a boolean, because trials split into treatment-naive and post-progression
 * populations and a bare "has taken this" matches both.
 *
 * <p>Distinct from {@link PatientMedicationDb}, which mirrors a FHIR MedicationRequest.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "patient_prior_treatment")
public class PatientPriorTreatmentDb extends BaseDb {

    private static final long serialVersionUID = 1234567890123456827L;

    @Column(name = "app_user_id", nullable = false)
    private Long appUserId;

    /** Nullable on purpose: must survive the diagnosis row being deleted and recreated. */
    @Column(name = "patient_diagnosis_id")
    private Long patientDiagnosisId;

    @Column(name = "cdk46_status", length = 24)
    private String cdk46Status;

    @Column(name = "endocrine_status", length = 24)
    private String endocrineStatus;

    @Column(name = "serd_status", length = 24)
    private String serdStatus;

    @Column(name = "chemo_status", length = 24)
    private String chemoStatus;

    @Column(name = "her2_therapy_status", length = 24)
    private String her2TherapyStatus;

    @Column(name = "her2_adc_status", length = 24)
    private String her2AdcStatus;

    @Column(name = "trop2_adc_status", length = 24)
    private String trop2AdcStatus;

    @Column(name = "parp_status", length = 24)
    private String parpStatus;

    @Column(name = "pi3k_akt_mtor_status", length = 24)
    private String pi3kAktMtorStatus;

    @Column(name = "immunotherapy_status", length = 24)
    private String immunotherapyStatus;

    @Column(name = "taxane_status", length = 24)
    private String taxaneStatus;

    @Column(name = "anthracycline_status", length = 24)
    private String anthracyclineStatus;

    @Column(name = "platinum_status", length = 24)
    private String platinumStatus;

    @Column(name = "current_drug_names", length = 1000)
    private String currentDrugNames;

    @Column(name = "prior_drug_names", length = 1000)
    private String priorDrugNames;

    /** 0 means treatment-naive in the metastatic setting - a meaningful value, not missing. */
    @Column(name = "lines_of_therapy_metastatic")
    private Integer linesOfTherapyMetastatic;

    @Column(name = "had_neoadjuvant")
    private Boolean hadNeoadjuvant;

    @Column(name = "had_adjuvant")
    private Boolean hadAdjuvant;

    @Column(name = "had_radiation")
    private Boolean hadRadiation;

    @Column(name = "had_surgery")
    private Boolean hadSurgery;

    @Column(name = "last_treatment_end_date")
    private LocalDate lastTreatmentEndDate;

    @Column(name = "currently_on_treatment")
    private Boolean currentlyOnTreatment;

    @Column(name = "other_treatments", length = 1000)
    private String otherTreatments;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;
}
