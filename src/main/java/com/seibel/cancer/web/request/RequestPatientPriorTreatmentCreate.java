package com.seibel.cancer.web.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * Cross-entity references are extids, never numeric ids. Every exposure field is optional -
 * an unanswered question is a first-class state, and requiring an answer would force a
 * false "never taken this".
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RequestPatientPriorTreatmentCreate extends BaseRequest {

    @NotEmpty(message = "The appUserExtid is required.")
    @Size(max = 36, message = "The appUserExtid must be at most 36 characters.")
    private String appUserExtid;

    @Size(max = 36, message = "The patientDiagnosisExtid must be at most 36 characters.")
    private String patientDiagnosisExtid;

    @Size(max = 24, message = "The cdk46Status must be at most 24 characters.")
    private String cdk46Status;

    @Size(max = 24, message = "The endocrineStatus must be at most 24 characters.")
    private String endocrineStatus;

    @Size(max = 24, message = "The serdStatus must be at most 24 characters.")
    private String serdStatus;

    @Size(max = 24, message = "The chemoStatus must be at most 24 characters.")
    private String chemoStatus;

    @Size(max = 24, message = "The her2TherapyStatus must be at most 24 characters.")
    private String her2TherapyStatus;

    @Size(max = 24, message = "The her2AdcStatus must be at most 24 characters.")
    private String her2AdcStatus;

    @Size(max = 24, message = "The trop2AdcStatus must be at most 24 characters.")
    private String trop2AdcStatus;

    @Size(max = 24, message = "The parpStatus must be at most 24 characters.")
    private String parpStatus;

    @Size(max = 24, message = "The pi3kAktMtorStatus must be at most 24 characters.")
    private String pi3kAktMtorStatus;

    @Size(max = 24, message = "The immunotherapyStatus must be at most 24 characters.")
    private String immunotherapyStatus;

    @Size(max = 24, message = "The taxaneStatus must be at most 24 characters.")
    private String taxaneStatus;

    @Size(max = 24, message = "The anthracyclineStatus must be at most 24 characters.")
    private String anthracyclineStatus;

    @Size(max = 24, message = "The platinumStatus must be at most 24 characters.")
    private String platinumStatus;

    @Size(max = 1000, message = "The currentDrugNames must be at most 1000 characters.")
    private String currentDrugNames;

    @Size(max = 1000, message = "The priorDrugNames must be at most 1000 characters.")
    private String priorDrugNames;

    @Min(value = 0, message = "The linesOfTherapyMetastatic must be at least 0.")
    private Integer linesOfTherapyMetastatic;

    private Boolean hadNeoadjuvant;
    private Boolean hadAdjuvant;
    private Boolean hadRadiation;
    private Boolean hadSurgery;
    private LocalDate lastTreatmentEndDate;
    private Boolean currentlyOnTreatment;

    @Size(max = 1000, message = "The otherTreatments must be at most 1000 characters.")
    private String otherTreatments;

    private String notes;
}
