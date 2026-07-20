package com.seibel.jobhunting.database.db.repository;

import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.database.db.entity.UserDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserDb, Long> {
    Optional<UserDb> findByUsername(String username);
    Optional<UserDb> findByExtid(String extid);
    List<UserDb> findByActive(ActiveEnum active);
    Page<UserDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByUsername(String username);
    boolean existsByExtid(String extid);

    default List<UserDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
