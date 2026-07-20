package com.seibel.jobhunting.database.db.repository;

import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.database.db.entity.FriendDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendRepository extends JpaRepository<FriendDb, Long> {
    Optional<FriendDb> findByExtid(String extid);
    List<FriendDb> findByActive(ActiveEnum active);
    Page<FriendDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    default List<FriendDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
