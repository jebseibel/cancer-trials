package com.seibel.jobs.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "skill")
public class SkillDb extends BaseDb {

    private static final long serialVersionUID = 1L;

    @Column(name = "name", length = 120, nullable = false, unique = true)
    private String name;
}
