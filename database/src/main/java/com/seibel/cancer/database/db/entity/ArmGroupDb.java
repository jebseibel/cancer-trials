package com.seibel.cancer.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "arm_group")
public class ArmGroupDb extends BaseDb {

    private static final long serialVersionUID = 1234567890123456800L;

    @Column(name = "trial_id", nullable = false)
    private Long trialId;

    @Column(name = "label", length = 255, nullable = false)
    private String label;

    @Column(name = "type", length = 255)
    private String type;

    @Column(name = "description", columnDefinition = "text")
    private String description;
}
