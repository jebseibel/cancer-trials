package com.seibel.cancer.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestPatientDiagnosisCreate extends BaseRequest {

    @Size(max = 36, message = "The patientExtid must be at most 36 characters.")
    private String patientExtid;

    @NotEmpty(message = "The cancerType is required.")
    @Size(max = 255, message = "The cancerType must be at most 255 characters.")
    private String cancerType;

    @Size(max = 16, message = "The stage must be at most 16 characters.")
    private String stage;

    @Size(max = 16, message = "The stageSystem must be at most 16 characters.")
    private String stageSystem;

    private Boolean isMetastatic;

    @Size(max = 500, message = "The metastasisSites must be at most 500 characters.")
    private String metastasisSites;

    @Size(max = 64, message = "The receptorSubtype must be at most 64 characters.")
    private String receptorSubtype;

    @Size(max = 16, message = "The erStatus must be at most 16 characters.")
    private String erStatus;

    @Size(max = 16, message = "The prStatus must be at most 16 characters.")
    private String prStatus;

    @Size(max = 16, message = "The her2Status must be at most 16 characters.")
    private String her2Status;

    @Size(max = 1000, message = "The biomarkers must be at most 1000 characters.")
    private String biomarkers;

    private Integer ecogStatus;

    private Integer priorChemoRegimens;

    private LocalDate lastChemoEndDate;

    @Size(max = 2000, message = "The priorTreatments must be at most 2000 characters.")
    private String priorTreatments;

    private Boolean hasMeasurableDisease;

    @Size(max = 16, message = "The menopausalStatus must be at most 16 characters.")
    private String menopausalStatus;



    private LocalDate diagnosisDate;

    private String notes;
}
