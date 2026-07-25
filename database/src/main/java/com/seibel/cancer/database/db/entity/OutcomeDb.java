package com.seibel.cancer.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "outcome")
public class OutcomeDb extends BaseDb {

    private static final long serialVersionUID = 1234567890123456800L;

    @Column(name = "trial_id", nullable = false)
    private Long trialId;

    @Column(name = "outcome_type", length = 16, nullable = false)
    private String outcomeType;

    @Column(name = "measure", length = 500, nullable = false)
    private String measure;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "time_frame", length = 255)
    private String timeFrame;
}
