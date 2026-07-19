package com.seibel.jobs.database.db.entity;

import com.seibel.jobs.common.enums.JobPostingStatus;
import com.seibel.jobs.common.enums.JobSource;
import com.seibel.jobs.common.enums.WorkMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "job_posting")
public class JobPostingDb extends BaseDb {

    private static final long serialVersionUID = 1L;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "city", length = 120)
    private String city;

    @Column(name = "state", length = 120)
    private String state;

    @Column(name = "country", length = 120)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode", length = 32)
    private WorkMode workMode;

    @Column(name = "salary_min")
    private Integer salaryMin;

    @Column(name = "salary_max")
    private Integer salaryMax;

    @Column(name = "salary_currency", length = 8)
    private String salaryCurrency;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", length = 32, nullable = false)
    private JobSource source;

    @Column(name = "source_url", length = 1024, nullable = false, unique = true)
    private String sourceUrl;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private JobPostingStatus status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
