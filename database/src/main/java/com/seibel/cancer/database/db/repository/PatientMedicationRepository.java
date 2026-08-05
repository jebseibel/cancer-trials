package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.PatientMedicationDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientMedicationRepository extends JpaRepository<PatientMedicationDb, Long> {
    Optional<PatientMedicationDb> findByExtid(String extid);
    List<PatientMedicationDb> findByActive(ActiveEnum active);
    Page<PatientMedicationDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    /** The natural key from Epic - the dedup lookup used when re-ingesting. */
    Optional<PatientMedicationDb> findByFhirResourceId(String fhirResourceId);

    List<PatientMedicationDb> findByStatus(String status);

    default List<PatientMedicationDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
