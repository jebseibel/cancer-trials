package com.seibel.cancer.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * A person with a medical record. <strong>Deliberately carries no credential fields</strong> -
 * see {@code com.seibel.cancer.common.domain.Patient} for why.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "patient")
public class PatientDb extends BaseDb {

    private static final long serialVersionUID = 1234567890123456812L;

    @Column(name = "display_name", length = 128, nullable = false)
    private String displayName;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "sex", length = 16)
    private String sex;

    @Column(name = "notes", length = 1000)
    private String notes;
}
