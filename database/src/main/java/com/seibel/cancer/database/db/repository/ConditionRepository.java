package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.MedicalConditionDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConditionRepository extends JpaRepository<MedicalConditionDb, Long> {
    Optional<MedicalConditionDb> findByExtid(String extid);
    Optional<MedicalConditionDb> findByName(String name);
    List<MedicalConditionDb> findByActive(ActiveEnum active);
    Page<MedicalConditionDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    default List<MedicalConditionDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
