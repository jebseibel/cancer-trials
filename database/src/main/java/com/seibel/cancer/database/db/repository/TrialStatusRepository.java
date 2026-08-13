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

    /**
     * Filters on active. This finder shipped without that filter and returned soft-deleted
     * rows - the same bug {@code PatientDiagnosisRepository.findByPatientId} had, fixed
     * 2026-08-08, and predicted for this one in {@code .claude/CURRENT_STATE.md}. A delete
     * that a read ignores is worse than no delete: it reports success and changes nothing
     * the caller can see.
     */
    List<TrialStatusDb> findByPatientIdAndActive(Long patientId, ActiveEnum active);

    default List<TrialStatusDb> findByPatientId(Long patientId) {
        return findByPatientIdAndActive(patientId, ActiveEnum.ACTIVE);
    }

    default List<TrialStatusDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
