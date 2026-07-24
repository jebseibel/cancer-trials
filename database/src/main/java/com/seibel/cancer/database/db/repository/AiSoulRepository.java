package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.ai.AiSoulDb;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiSoulRepository extends ListCrudRepository<AiSoulDb, Long> {
    Optional<AiSoulDb> findByExtid(String extid);
    List<AiSoulDb> findByActive(ActiveEnum active);
    boolean existsByExtid(String extid);
    boolean existsByUniqueId(String uniqueId);
    Optional<AiSoulDb> findByUniqueId(String uniqueId);

    default List<AiSoulDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
