package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ResponsePatientDiagnosis {
    private String extid;
    private String appUserExtid;
    private String cancerType;
    private String stage;
    private String stageSystem;
    private Boolean isMetastatic;
    private String metastasisSites;
    private String receptorSubtype;
    private String erStatus;
    private String prStatus;
    private String her2Status;
    private String biomarkers;
    private Integer ecogStatus;
    private Integer priorChemoRegimens;
    private LocalDate lastChemoEndDate;
    private String priorTreatments;
    private Boolean hasMeasurableDisease;
    private String menopausalStatus;
    private LocalDate dateOfBirth;
    private String sex;
    private LocalDate diagnosisDate;
    private String notes;
}
