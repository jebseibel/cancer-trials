package com.seibel.jobs.database.db.repository;

import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.database.db.entity.JobPostingSkillDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobPostingSkillRepository extends JpaRepository<JobPostingSkillDb, Long> {
    Optional<JobPostingSkillDb> findByExtid(String extid);
    List<JobPostingSkillDb> findByJobPostingId(Long jobPostingId);
    List<JobPostingSkillDb> findBySkillId(Long skillId);
    List<JobPostingSkillDb> findByActive(ActiveEnum active);
    Page<JobPostingSkillDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    default List<JobPostingSkillDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
