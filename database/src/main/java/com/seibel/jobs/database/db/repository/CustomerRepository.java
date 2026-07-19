package com.seibel.jobs.database.db.repository;

import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.database.db.entity.CustomerDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<CustomerDb, Long> {
    Optional<CustomerDb> findByName(String name);
    Optional<CustomerDb> findByExtid(String extid);
    List<CustomerDb> findByActive(ActiveEnum active);
    Page<CustomerDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    default List<CustomerDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
