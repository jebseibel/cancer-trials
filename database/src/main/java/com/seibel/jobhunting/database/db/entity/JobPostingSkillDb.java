package com.seibel.jobhunting.database.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "job_posting_skill", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"job_posting_id", "skill_id"})
})
public class JobPostingSkillDb extends BaseDb {

    private static final long serialVersionUID = 1L;

    @Column(name = "job_posting_id", nullable = false)
    private Long jobPostingId;

    @Column(name = "skill_id", nullable = false)
    private Long skillId;
}
