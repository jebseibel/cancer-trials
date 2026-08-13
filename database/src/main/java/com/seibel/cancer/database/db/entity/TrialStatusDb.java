package com.seibel.cancer.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "trial_status")
public class TrialStatusDb extends BaseDb {

    private static final long serialVersionUID = 1074658231345679012L;

    @Column(name = "trial_id", nullable = false)
    private Long trialId;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "status", length = 16, nullable = false)
    private String status;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "status_changed_at")
    private LocalDateTime statusChangedAt;
}
