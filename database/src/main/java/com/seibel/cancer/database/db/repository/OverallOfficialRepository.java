package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.OverallOfficialDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OverallOfficialRepository extends JpaRepository<OverallOfficialDb, Long> {
    Optional<OverallOfficialDb> findByExtid(String extid);
    List<OverallOfficialDb> findByActive(ActiveEnum active);
    Page<OverallOfficialDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);
    List<OverallOfficialDb> findByTrialId(Long trialId);

    default List<OverallOfficialDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
