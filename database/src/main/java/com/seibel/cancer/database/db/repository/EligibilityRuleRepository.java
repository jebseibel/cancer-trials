package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.EligibilityRuleDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EligibilityRuleRepository extends JpaRepository<EligibilityRuleDb, Long> {
    Optional<EligibilityRuleDb> findByExtid(String extid);
    List<EligibilityRuleDb> findByActive(ActiveEnum active);
    Page<EligibilityRuleDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);
    List<EligibilityRuleDb> findByTrialId(Long trialId);

    default List<EligibilityRuleDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
