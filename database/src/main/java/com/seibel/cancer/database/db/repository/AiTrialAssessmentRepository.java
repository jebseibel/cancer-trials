package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.AiTrialAssessmentDb;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiTrialAssessmentRepository extends JpaRepository<AiTrialAssessmentDb, Long> {

    Optional<AiTrialAssessmentDb> findByExtid(String extid);

    boolean existsByExtid(String extid);

    /**
     * Every reading of this trial for this patient, newest first.
     *
     * <p>Filters on active so soft-deleted rows never surface — the same gap that once let the
     * Diagnosis page display and edit a deleted record.
     *
     * <p>Returns the history rather than just the latest: a re-check inserts, and comparing two
     * readings is how you tell a changed answer from changed circumstances.
     */
    List<AiTrialAssessmentDb> findByTrialIdAndPatientIdAndActiveOrderByAssessedAtDesc(
            Long trialId, Long patientId, ActiveEnum active);
}
