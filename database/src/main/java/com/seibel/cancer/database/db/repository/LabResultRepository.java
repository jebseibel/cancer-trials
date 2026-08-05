package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.LabResultDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LabResultRepository extends JpaRepository<LabResultDb, Long> {
    Optional<LabResultDb> findByExtid(String extid);
    List<LabResultDb> findByActive(ActiveEnum active);
    Page<LabResultDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    /** The natural key from Epic - the dedup lookup used when re-ingesting. */
    Optional<LabResultDb> findByFhirResourceId(String fhirResourceId);

    /** All results for one test, newest first - the "this lab over time" query. */
    List<LabResultDb> findByLoincCodeOrderByEffectiveAtDesc(String loincCode);

    default List<LabResultDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
