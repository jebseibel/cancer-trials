package com.seibel.cancer.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * Molecular and germline findings for one patient.
 *
 * <p>Status columns use {@code DETECTED | NOT_DETECTED | VUS | NOT_TESTED | UNKNOWN}. Nothing
 * here defaults to a negative - "not tested" and "tested negative" are clinically different
 * and collapsing them either hides a trial option or invents a qualification.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "patient_variant")
public class PatientVariantDb extends BaseDb {

    private static final long serialVersionUID = 1234567890123456826L;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    /** Nullable on purpose: a variant row must survive the diagnosis being deleted and recreated. */
    @Column(name = "patient_diagnosis_id")
    private Long patientDiagnosisId;

    @Column(name = "pik3ca_status", length = 16)
    private String pik3caStatus;

    @Column(name = "esr1_status", length = 16)
    private String esr1Status;

    @Column(name = "tp53_status", length = 16)
    private String tp53Status;

    @Column(name = "akt1_status", length = 16)
    private String akt1Status;

    @Column(name = "pten_status", length = 16)
    private String ptenStatus;

    /** Somatic ERBB2 mutation - a different test from HER2 receptor status on patient_diagnosis. */
    @Column(name = "erbb2_somatic_status", length = 16)
    private String erbb2SomaticStatus;

    @Column(name = "brca1_status", length = 16)
    private String brca1Status;

    @Column(name = "brca2_status", length = 16)
    private String brca2Status;

    @Column(name = "palb2_status", length = 16)
    private String palb2Status;

    @Column(name = "atm_status", length = 16)
    private String atmStatus;

    @Column(name = "chek2_status", length = 16)
    private String chek2Status;

    @Column(name = "hrd_status", length = 16)
    private String hrdStatus;

    @Column(name = "pdl1_status", length = 16)
    private String pdl1Status;

    @Column(name = "ki67_percent")
    private Integer ki67Percent;

    @Column(name = "germline_test_done", length = 16)
    private String germlineTestDone;

    @Column(name = "somatic_test_done", length = 16)
    private String somaticTestDone;

    @Column(name = "test_date")
    private LocalDate testDate;

    @Column(name = "test_lab", length = 255)
    private String testLab;

    @Column(name = "other_variants", length = 1000)
    private String otherVariants;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;
}
