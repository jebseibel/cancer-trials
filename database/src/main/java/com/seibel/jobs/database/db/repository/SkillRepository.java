package com.seibel.jobs.database.db.repository;

import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.database.db.entity.SkillDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<SkillDb, Long> {
    Optional<SkillDb> findByName(String name);
    Optional<SkillDb> findByExtid(String extid);
    List<SkillDb> findByActive(ActiveEnum active);
    Page<SkillDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    default List<SkillDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
