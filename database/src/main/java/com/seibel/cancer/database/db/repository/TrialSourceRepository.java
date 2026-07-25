package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.TrialSourceDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrialSourceRepository extends JpaRepository<TrialSourceDb, Long> {
    Optional<TrialSourceDb> findByExtid(String extid);
    List<TrialSourceDb> findByActive(ActiveEnum active);
    Page<TrialSourceDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);
    Optional<TrialSourceDb> findByCode(String code);

    default List<TrialSourceDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
