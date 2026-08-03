package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.StagingRawTrialDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StagingRawTrialRepository extends JpaRepository<StagingRawTrialDb, Long> {
    Optional<StagingRawTrialDb> findByExtid(String extid);
    List<StagingRawTrialDb> findByActive(ActiveEnum active);
    Page<StagingRawTrialDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    List<StagingRawTrialDb> findBySourceTrialId(String sourceTrialId);
    Optional<StagingRawTrialDb> findByTrialSourceIdAndSourceTrialId(Long trialSourceId, String sourceTrialId);
    List<StagingRawTrialDb> findByNormalizedAtIsNullAndActive(ActiveEnum active, Pageable pageable);

    default List<StagingRawTrialDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
