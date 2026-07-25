package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.TrialDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrialRepository extends JpaRepository<TrialDb, Long> {
    Optional<TrialDb> findByExtid(String extid);
    Optional<TrialDb> findByNctId(String nctId);
    List<TrialDb> findByActive(ActiveEnum active);
    Page<TrialDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    default List<TrialDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
