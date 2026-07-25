package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.TrialStatusDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrialStatusRepository extends JpaRepository<TrialStatusDb, Long> {
    Optional<TrialStatusDb> findByExtid(String extid);
    List<TrialStatusDb> findByActive(ActiveEnum active);
    Page<TrialStatusDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);
    List<TrialStatusDb> findByTrialId(Long trialId);
    List<TrialStatusDb> findByAppUserId(Long appUserId);

    default List<TrialStatusDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
