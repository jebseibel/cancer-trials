package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.AppUserDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUserDb, Long> {
    Optional<AppUserDb> findByExtid(String extid);
    Optional<AppUserDb> findByUsername(String username);
    List<AppUserDb> findByActive(ActiveEnum active);
    Page<AppUserDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    default List<AppUserDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
