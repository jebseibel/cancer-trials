package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.PatientVariantDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientVariantRepository extends JpaRepository<PatientVariantDb, Long> {
    Optional<PatientVariantDb> findByExtid(String extid);
    List<PatientVariantDb> findByActive(ActiveEnum active);
    Page<PatientVariantDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    /**
     * The patient's variant row, newest first.
     *
     * Filters on active: PatientDiagnosisRepository.findByAppUserId shipped without that
     * filter and the Diagnosis page displayed and edited a soft-deleted record. Same shape
     * here, so the same guard from the start.
     */
    List<PatientVariantDb> findByAppUserIdAndActiveOrderByCreatedAtDesc(Long appUserId, ActiveEnum active);

    default List<PatientVariantDb> findByAppUserId(Long appUserId) {
        return findByAppUserIdAndActiveOrderByCreatedAtDesc(appUserId, ActiveEnum.ACTIVE);
    }

    List<PatientVariantDb> findByPatientDiagnosisIdAndActive(Long patientDiagnosisId, ActiveEnum active);

    default List<PatientVariantDb> findByPatientDiagnosisId(Long patientDiagnosisId) {
        return findByPatientDiagnosisIdAndActive(patientDiagnosisId, ActiveEnum.ACTIVE);
    }

    default List<PatientVariantDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
