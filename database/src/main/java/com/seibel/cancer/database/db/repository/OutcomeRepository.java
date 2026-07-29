package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.OutcomeDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OutcomeRepository extends JpaRepository<OutcomeDb, Long> {
    Optional<OutcomeDb> findByExtid(String extid);
    List<OutcomeDb> findByActive(ActiveEnum active);
    Page<OutcomeDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);
    List<OutcomeDb> findByTrialId(Long trialId);

    default List<OutcomeDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
