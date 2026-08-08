package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.PatientDiagnosisDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientDiagnosisRepository extends JpaRepository<PatientDiagnosisDb, Long> {
    Optional<PatientDiagnosisDb> findByExtid(String extid);
    List<PatientDiagnosisDb> findByActive(ActiveEnum active);
    Page<PatientDiagnosisDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    /** One patient's diagnosis is looked up by user - realistically a single row. */
    List<PatientDiagnosisDb> findByAppUserId(Long appUserId);

    default List<PatientDiagnosisDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
