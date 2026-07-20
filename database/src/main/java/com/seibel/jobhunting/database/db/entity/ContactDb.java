package com.seibel.jobhunting.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "contact")
public class ContactDb extends BaseDb {

    private static final long serialVersionUID = 1L;

    @Column(name = "company_id")
    private Long companyId;

    @Column(name = "job_posting_id")
    private Long jobPostingId;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "role", length = 120)
    private String role;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 32)
    private String phone;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
