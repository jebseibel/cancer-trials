package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * What the frontend sees of a diagnosis-intake session: the draft so far, grouped exactly like
 * {@code ResponsePatientDiagnosis} / {@code ResponsePatientVariant} /
 * {@code ResponsePatientPriorTreatment} minus {@code extid}, {@code patientExtid},
 * {@code receptorSubtype}, {@code notes}, and {@code testLab} - none of those are ever drafted
 * by this feature.
 */
@Data
@Builder
public class ResponseDiagnosisIntakeSession {

    private String sessionId;
    private String status;

    private DraftDiagnosis draftDiagnosis;
    private DraftVariant draftVariant;
    private DraftPriorTreatment draftPriorTreatment;

    private List<String> missingRequiredFields;
    private String nextQuestion;
    private int turnCount;

    /** Only ever populated by {@code /start} - a clarifying answer never goes through the PHI
     * line scan, so there is nothing to report on later turns. Empty, never null, when nothing
     * was cut. Reasons are category labels only, same rule the scan itself follows - never the
     * excluded text. */
    @Builder.Default
    private List<ExcludedLine> excludedLines = List.of();

    @Data
    @Builder
    public static class ExcludedLine {
        private int lineNumber;
        private List<String> reasons;
    }

    @Data
    @Builder
    public static class DraftDiagnosis {
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
    }

    @Data
    @Builder
    public static class DraftVariant {
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
    }

    @Data
    @Builder
    public static class DraftPriorTreatment {
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
    }
}
