package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.SponsorDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SponsorRepository extends JpaRepository<SponsorDb, Long> {
    Optional<SponsorDb> findByExtid(String extid);
    Optional<SponsorDb> findByName(String name);
    List<SponsorDb> findByActive(ActiveEnum active);
    Page<SponsorDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    default List<SponsorDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
