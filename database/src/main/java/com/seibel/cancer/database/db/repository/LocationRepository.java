package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.LocationDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<LocationDb, Long> {
    Optional<LocationDb> findByExtid(String extid);
    List<LocationDb> findByActive(ActiveEnum active);
    Page<LocationDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);
    List<LocationDb> findByTrialId(Long trialId);

    default List<LocationDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
