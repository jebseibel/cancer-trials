package com.seibel.jobs.database.db.entity;

import com.seibel.jobs.common.enums.ApplicationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "application")
public class ApplicationDb extends BaseDb {

    private static final long serialVersionUID = 1L;

    @Column(name = "job_posting_id", nullable = false)
    private Long jobPostingId;

    @Column(name = "date_applied", nullable = false)
    private LocalDate dateApplied;

    @Column(name = "resume_version", length = 120)
    private String resumeVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "application_status", length = 32, nullable = false)
    private ApplicationStatus applicationStatus;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
