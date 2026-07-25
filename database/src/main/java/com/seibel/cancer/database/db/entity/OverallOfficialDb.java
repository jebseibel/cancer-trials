package com.seibel.cancer.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "overall_official")
public class OverallOfficialDb extends BaseDb {

    private static final long serialVersionUID = 1234567890123456801L;

    @Column(name = "trial_id", nullable = false)
    private Long trialId;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "affiliation", length = 255)
    private String affiliation;

    @Column(name = "role", length = 100)
    private String role;
}
