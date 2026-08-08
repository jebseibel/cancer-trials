package com.seibel.cancer.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "patient_diagnosis")
public class PatientDiagnosisDb extends BaseDb {

    private static final long serialVersionUID = 1234567890123456823L;

    // FK -> app_user.id. Plain Long, no @ManyToOne, per the project convention.
    @Column(name = "app_user_id")
    private Long appUserId;

    @Column(name = "cancer_type", length = 255, nullable = false)
    private String cancerType;

    @Column(name = "stage", length = 16)
    private String stage;

    @Column(name = "stage_system", length = 16)
    private String stageSystem;

    @Column(name = "is_metastatic")
    private Boolean isMetastatic;

    @Column(name = "metastasis_sites", length = 500)
    private String metastasisSites;

    @Column(name = "receptor_subtype", length = 64)
    private String receptorSubtype;

    @Column(name = "er_status", length = 16)
    private String erStatus;

    @Column(name = "pr_status", length = 16)
    private String prStatus;

    @Column(name = "her2_status", length = 16)
    private String her2Status;

    @Column(name = "biomarkers", length = 1000)
    private String biomarkers;

    @Column(name = "ecog_status")
    private Integer ecogStatus;

    @Column(name = "prior_chemo_regimens")
    private Integer priorChemoRegimens;

    @Column(name = "last_chemo_end_date")
    private LocalDate lastChemoEndDate;

    @Column(name = "prior_treatments", length = 2000)
    private String priorTreatments;

    @Column(name = "has_measurable_disease")
    private Boolean hasMeasurableDisease;

    @Column(name = "menopausal_status", length = 16)
    private String menopausalStatus;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "sex", length = 16)
    private String sex;

    @Column(name = "diagnosis_date")
    private LocalDate diagnosisDate;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;
}
