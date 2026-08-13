package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ResponsePatientPriorTreatment {
    private String extid;
    private String patientExtid;
    private String patientDiagnosisExtid;
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
    private LocalDate lastTreatmentEndDate;
    private Boolean currentlyOnTreatment;
    private String otherTreatments;
    private String notes;
}
