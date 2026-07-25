package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.MedicationDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicationRepository extends JpaRepository<MedicationDb, Long> {
    Optional<MedicationDb> findByExtid(String extid);
    Optional<MedicationDb> findByName(String name);
    List<MedicationDb> findByActive(ActiveEnum active);
    Page<MedicationDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    default List<MedicationDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
