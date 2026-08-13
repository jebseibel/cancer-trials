package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.SavedTrialMatchDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedTrialMatchRepository extends JpaRepository<SavedTrialMatchDb, Long> {
    Optional<SavedTrialMatchDb> findByExtid(String extid);
    List<SavedTrialMatchDb> findByActive(ActiveEnum active);
    Page<SavedTrialMatchDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    /**
     * Every match from one search run, highest-ranked first. This is the primary read:
     * a run is the unit a user looks at.
     *
     * Filters on active so soft-deleted rows never surface - the same gap that let
     * PatientDiagnosis return a deleted row to the Diagnosis page.
     */
    List<SavedTrialMatchDb> findBySearchRunIdAndActiveOrderByMatchRankAsc(String searchRunId, ActiveEnum active);

    default List<SavedTrialMatchDb> findBySearchRunId(String searchRunId) {
        return findBySearchRunIdAndActiveOrderByMatchRankAsc(searchRunId, ActiveEnum.ACTIVE);
    }

    /** Every match ever recorded for one user, newest first - the history view. */
    List<SavedTrialMatchDb> findByPatientIdAndActiveOrderByMatchedAtDesc(Long patientId, ActiveEnum active);

    default List<SavedTrialMatchDb> findByPatientId(Long patientId) {
        return findByPatientIdAndActiveOrderByMatchedAtDesc(patientId, ActiveEnum.ACTIVE);
    }

    /** Every run this trial has appeared in - "why did this surface, and when?" */
    List<SavedTrialMatchDb> findByTrialIdAndActiveOrderByMatchedAtDesc(Long trialId, ActiveEnum active);

    default List<SavedTrialMatchDb> findByTrialId(Long trialId) {
        return findByTrialIdAndActiveOrderByMatchedAtDesc(trialId, ActiveEnum.ACTIVE);
    }

    default List<SavedTrialMatchDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
