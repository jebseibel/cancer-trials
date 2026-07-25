package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.InterventionDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterventionRepository extends JpaRepository<InterventionDb, Long> {
    Optional<InterventionDb> findByExtid(String extid);
    List<InterventionDb> findByActive(ActiveEnum active);
    Page<InterventionDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    List<InterventionDb> findByTrialId(Long trialId);

    default List<InterventionDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
