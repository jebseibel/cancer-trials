package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.SavedTrialMatchCriterionDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedTrialMatchCriterionRepository extends JpaRepository<SavedTrialMatchCriterionDb, Long> {
    Optional<SavedTrialMatchCriterionDb> findByExtid(String extid);
    List<SavedTrialMatchCriterionDb> findByActive(ActiveEnum active);
    Page<SavedTrialMatchCriterionDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    /** The evidence behind one match, best-scoring first. Filters soft-deleted rows. */
    List<SavedTrialMatchCriterionDb> findByTrialMatchIdAndActiveOrderByScoreDesc(Long trialMatchId, ActiveEnum active);

    default List<SavedTrialMatchCriterionDb> findByTrialMatchId(Long trialMatchId) {
        return findByTrialMatchIdAndActiveOrderByScoreDesc(trialMatchId, ActiveEnum.ACTIVE);
    }

    default List<SavedTrialMatchCriterionDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
