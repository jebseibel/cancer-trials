package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.StagingRawFhirResourceDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StagingRawFhirResourceRepository extends JpaRepository<StagingRawFhirResourceDb, Long> {
    Optional<StagingRawFhirResourceDb> findByExtid(String extid);
    List<StagingRawFhirResourceDb> findByActive(ActiveEnum active);
    Page<StagingRawFhirResourceDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    List<StagingRawFhirResourceDb> findByFhirResourceId(String fhirResourceId);
    Optional<StagingRawFhirResourceDb> findByResourceTypeAndFhirResourceId(String resourceType, String fhirResourceId);
    List<StagingRawFhirResourceDb> findByNormalizedAtIsNullAndActive(ActiveEnum active, Pageable pageable);

    default List<StagingRawFhirResourceDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
