package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.LabResultComponentDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LabResultComponentRepository extends JpaRepository<LabResultComponentDb, Long> {
    Optional<LabResultComponentDb> findByExtid(String extid);
    List<LabResultComponentDb> findByActive(ActiveEnum active);
    Page<LabResultComponentDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    /** All components of one panel - also the lookup used when re-normalizing a parent. */
    List<LabResultComponentDb> findByLabResultId(Long labResultId);

    default List<LabResultComponentDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
