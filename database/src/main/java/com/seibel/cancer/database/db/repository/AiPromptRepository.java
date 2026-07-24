package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.domain.enums.ai.AiLifecycle;
import com.seibel.cancer.database.db.entity.ai.AiPromptDb;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiPromptRepository extends ListCrudRepository<AiPromptDb, Long> {
    Optional<AiPromptDb> findByExtid(String extid);
    List<AiPromptDb> findByActive(ActiveEnum active);
    boolean existsByExtid(String extid);
    boolean existsByUniqueId(String uniqueId);
    Optional<AiPromptDb> findByUniqueId(String uniqueId);

    Optional<AiPromptDb> findByUniqueIdAndLifecycle(String uniqueId, AiLifecycle lifecycle);

    default List<AiPromptDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
