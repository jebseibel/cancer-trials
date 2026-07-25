package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.ConditionDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConditionRepository extends JpaRepository<ConditionDb, Long> {
    Optional<ConditionDb> findByExtid(String extid);
    Optional<ConditionDb> findByName(String name);
    List<ConditionDb> findByActive(ActiveEnum active);
    Page<ConditionDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    default List<ConditionDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
