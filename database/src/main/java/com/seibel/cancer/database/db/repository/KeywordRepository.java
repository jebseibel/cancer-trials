package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.KeywordDb;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KeywordRepository extends JpaRepository<KeywordDb, Long> {
    Optional<KeywordDb> findByExtid(String extid);
    Optional<KeywordDb> findByName(String name);
    List<KeywordDb> findByActive(ActiveEnum active);
    Page<KeywordDb> findByActive(ActiveEnum active, Pageable pageable);
    boolean existsByExtid(String extid);

    default List<KeywordDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
