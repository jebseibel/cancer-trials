package com.seibel.jobhunting.database.db.repository;

import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.database.db.entity.UserSkillDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSkillRepository extends JpaRepository<UserSkillDb, Long> {
    Optional<UserSkillDb> findByExtid(String extid);
    List<UserSkillDb> findByUserId(Long userId);
    List<UserSkillDb> findBySkillId(Long skillId);
    List<UserSkillDb> findByActive(ActiveEnum active);
    Page<UserSkillDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    default List<UserSkillDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
