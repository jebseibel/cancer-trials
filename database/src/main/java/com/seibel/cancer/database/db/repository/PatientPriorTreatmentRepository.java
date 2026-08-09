package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.PatientPriorTreatmentDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientPriorTreatmentRepository extends JpaRepository<PatientPriorTreatmentDb, Long> {
    Optional<PatientPriorTreatmentDb> findByExtid(String extid);
    List<PatientPriorTreatmentDb> findByActive(ActiveEnum active);
    Page<PatientPriorTreatmentDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    /**
     * The patient's treatment row, newest first.
     *
     * Filters on active: PatientDiagnosisRepository.findByAppUserId shipped without that
     * filter and the Diagnosis page displayed and edited a soft-deleted record. Same shape
     * here, so the same guard from the start.
     */
    List<PatientPriorTreatmentDb> findByAppUserIdAndActiveOrderByCreatedAtDesc(Long appUserId, ActiveEnum active);

    default List<PatientPriorTreatmentDb> findByAppUserId(Long appUserId) {
        return findByAppUserIdAndActiveOrderByCreatedAtDesc(appUserId, ActiveEnum.ACTIVE);
    }

    List<PatientPriorTreatmentDb> findByPatientDiagnosisIdAndActive(Long patientDiagnosisId, ActiveEnum active);

    default List<PatientPriorTreatmentDb> findByPatientDiagnosisId(Long patientDiagnosisId) {
        return findByPatientDiagnosisIdAndActive(patientDiagnosisId, ActiveEnum.ACTIVE);
    }

    default List<PatientPriorTreatmentDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
