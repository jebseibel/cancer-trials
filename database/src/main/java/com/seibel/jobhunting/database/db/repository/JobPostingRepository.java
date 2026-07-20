package com.seibel.jobhunting.database.db.repository;

import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.database.db.entity.JobPostingDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JobPostingRepository extends JpaRepository<JobPostingDb, Long> {
    Optional<JobPostingDb> findBySourceUrl(String sourceUrl);
    Optional<JobPostingDb> findByExtid(String extid);
    List<JobPostingDb> findByActive(ActiveEnum active);
    Page<JobPostingDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    default List<JobPostingDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
