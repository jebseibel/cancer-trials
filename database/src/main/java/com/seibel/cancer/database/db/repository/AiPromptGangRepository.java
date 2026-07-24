package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.domain.enums.ai.AiLifecycle;
import com.seibel.cancer.database.db.entity.ai.AiPromptGangDb;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiPromptGangRepository extends ListCrudRepository<AiPromptGangDb, Long> {
    Optional<AiPromptGangDb> findByExtid(String extid);
    Optional<AiPromptGangDb> findByName(String name);
    List<AiPromptGangDb> findByActive(ActiveEnum active);
    boolean existsByExtid(String extid);
    boolean existsByUniqueId(String uniqueId);
    Optional<AiPromptGangDb> findByUniqueId(String uniqueId);

    Optional<AiPromptGangDb> findByUniqueIdAndLifecycle(String uniqueId, AiLifecycle lifecycle);

    default List<AiPromptGangDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
