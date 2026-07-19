package com.seibel.jobs.database.db.repository;

import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.database.db.entity.FriendSkillDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendSkillRepository extends JpaRepository<FriendSkillDb, Long> {
    Optional<FriendSkillDb> findByExtid(String extid);
    List<FriendSkillDb> findByFriendId(Long friendId);
    List<FriendSkillDb> findBySkillId(Long skillId);
    List<FriendSkillDb> findByActive(ActiveEnum active);
    Page<FriendSkillDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    default List<FriendSkillDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
