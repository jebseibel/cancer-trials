package com.seibel.cancer.service.ai.intake;

import lombok.Data;

import java.util.List;

/**
 * What {@link DiagnosisIntakeExtractionService} asks the model to fill in from an uploaded
 * document - the extraction-target allowlist for this feature.
 *
 * <p>Field names match {@code PatientDiagnosis}, {@code PatientVariant}, and
 * {@code PatientPriorTreatment} 1:1 on purpose, so a field here reads as an instruction to the
 * model (per {@code AiService#generateStructured}'s javadoc) and maps onto the existing request
 * DTOs with no translation layer.
 *
 * <p><b>Deliberately excluded, and must stay excluded:</b> {@code notes} on all three tables,
 * {@code testLab} on the variant, and {@code receptorSubtype} (frontend-derived only, never
 * sent or received here). This mirrors {@code TrialDiagnosisMatchService}'s allowlist
 * discipline, applied in the opposite direction - an allowlist of what the model may fill in,
 * not what may be sent to it.
 */
@Data
public class DiagnosisIntakeExtraction {

    // ---- PatientDiagnosis subset ----
    private String cancerType;
    private String stage;
    private String stageSystem;
    private Boolean isMetastatic;
    private String metastasisSites;
    private String erStatus;
    private String prStatus;
    private String her2Status;
    private String biomarkers;
    private Integer ecogStatus;
    private Integer priorChemoRegimens;
    private String lastChemoEndDate;
    private String priorTreatments;
    private Boolean hasMeasurableDisease;
    private String menopausalStatus;
    private String diagnosisDate;

    // ---- PatientVariant subset ----
    private String pik3caStatus;
    private String esr1Status;
    private String tp53Status;
    private String akt1Status;
    private String ptenStatus;
    private String erbb2SomaticStatus;
    private String brca1Status;
    private String brca2Status;
    private String palb2Status;
    private String atmStatus;
    private String chek2Status;
    private String hrdStatus;
    private String pdl1Status;
    private Integer ki67Percent;
    private String germlineTestDone;
    private String somaticTestDone;
    private String testDate;
    private String otherVariants;

    // ---- PatientPriorTreatment subset ----
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
    private String taxaneStatus;
    private String anthracyclineStatus;
    private String platinumStatus;
    private String currentDrugNames;
    private String priorDrugNames;
    private Integer linesOfTherapyMetastatic;
    private Boolean hadNeoadjuvant;
    private Boolean hadAdjuvant;
    private Boolean hadRadiation;
    private Boolean hadSurgery;
    private String lastTreatmentEndDate;
    private Boolean currentlyOnTreatment;
    private String otherTreatments;

    /** The model's own hedge list - fields it saw mentioned but could not confidently resolve. */
    private List<String> fieldsMentionedButUnclear;
}
