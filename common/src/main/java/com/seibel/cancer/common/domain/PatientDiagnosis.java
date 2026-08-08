package com.seibel.cancer.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PatientDiagnosis extends BaseDomain {
    private Long appUserId;
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
