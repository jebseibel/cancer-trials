package com.seibel.cancer.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * Molecular and germline findings for one patient - one row per patient.
 *
 * <p>Every status field uses the same five-state vocabulary:
 * {@code DETECTED | NOT_DETECTED | VUS | NOT_TESTED | UNKNOWN}. "Not tested" is never
 * collapsed into "negative" - the distinction decides whether a trial is a genuine mismatch
 * or an open question worth asking an oncologist about.
 *
 * <p>Design and rationale in {@code .claude/diagnosis/patient-variant-and-treatment-tables.md}.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PatientVariant extends BaseDomain {
    private Long patientId;
    private Long patientDiagnosisId;

    // Somatic (tumor) findings
    private String pik3caStatus;
    private String esr1Status;
    private String tp53Status;
    private String akt1Status;
    private String ptenStatus;
    /** Somatic ERBB2 mutation - distinct from HER2 receptor status on PatientDiagnosis. */
    private String erbb2SomaticStatus;

    // Germline (inherited) findings
    private String brca1Status;
    private String brca2Status;
    private String palb2Status;
    private String atmStatus;
    private String chek2Status;

    // Composite and supporting biomarkers
    private String hrdStatus;
    private String pdl1Status;
    private Integer ki67Percent;

    // Provenance and escape hatch
    private String germlineTestDone;
    private String somaticTestDone;
    private LocalDate testDate;
    private String testLab;
    private String otherVariants;
    private String notes;
}
