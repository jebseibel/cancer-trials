package com.seibel.jobs.database.db.repository;

import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.database.db.entity.CompanyDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<CompanyDb, Long> {
    Optional<CompanyDb> findByName(String name);
    Optional<CompanyDb> findByExtid(String extid);
    List<CompanyDb> findByActive(ActiveEnum active);
    Page<CompanyDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    default List<CompanyDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
