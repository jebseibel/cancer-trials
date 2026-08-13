package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.PatientDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<PatientDb, Long> {

    Optional<PatientDb> findByExtid(String extid);

    List<PatientDb> findByActive(ActiveEnum active);

    Page<PatientDb> findByActive(ActiveEnum active, Pageable pageable);

    boolean existsByExtid(String extid);

    default List<PatientDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
