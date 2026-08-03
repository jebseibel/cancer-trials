package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.ArmGroupDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArmGroupRepository extends JpaRepository<ArmGroupDb, Long> {
    Optional<ArmGroupDb> findByExtid(String extid);
    List<ArmGroupDb> findByActive(ActiveEnum active);
    Page<ArmGroupDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);
    List<ArmGroupDb> findByTrialId(Long trialId);
    List<ArmGroupDb> findByTrialIdAndActive(Long trialId, ActiveEnum active);

    default List<ArmGroupDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
