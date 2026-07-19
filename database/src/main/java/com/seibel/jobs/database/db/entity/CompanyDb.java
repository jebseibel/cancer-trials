package com.seibel.jobs.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "company")
public class CompanyDb extends BaseDb {

    private static final long serialVersionUID = 1L;

    @Column(name = "name", length = 255, nullable = false, unique = true)
    private String name;

    @Column(name = "website", length = 1024)
    private String website;

    @Column(name = "industry", length = 120)
    private String industry;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
