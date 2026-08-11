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
    List<LocationDb> findByTrialIdAndActive(Long trialId, ActiveEnum active);

    /**
     * Locations for many trials in one query.
     *
     * <p>Exists for trial matching, which assesses thousands of trials per request and needs a
     * location signal on each. Fetching them one trial at a time cost ~2,000 round trips and 43
     * seconds; this is the same shape as the per-record queries that make normalization 99.3%
     * of an ingestion run.
     *
     * <p>Callers must chunk large id lists — MySQL's placeholder limit makes an unbounded
     * {@code IN} clause fail on a full-corpus call.
     */
    List<LocationDb> findByTrialIdInAndActive(List<Long> trialIds, ActiveEnum active);

    default List<LocationDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
