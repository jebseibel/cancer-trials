package com.seibel.jobhunting.database.db.repository;

import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.database.db.entity.FriendCompanyDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendCompanyRepository extends JpaRepository<FriendCompanyDb, Long> {
    Optional<FriendCompanyDb> findByExtid(String extid);
    List<FriendCompanyDb> findByFriendId(Long friendId);
    List<FriendCompanyDb> findByCompanyId(Long companyId);
    List<FriendCompanyDb> findByActive(ActiveEnum active);
    Page<FriendCompanyDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    default List<FriendCompanyDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
