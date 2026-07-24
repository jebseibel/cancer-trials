package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.enums.ai.AiLifecycle;
import com.seibel.cancer.database.db.entity.ai.AiPromptEnvelopeDb;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiPromptEnvelopeRepository extends ListCrudRepository<AiPromptEnvelopeDb, Long> {
    Optional<AiPromptEnvelopeDb> findByExtid(String extid);
    Optional<AiPromptEnvelopeDb> findByName(String name);
    List<AiPromptEnvelopeDb> findByActive(ActiveEnum active);
    boolean existsByExtid(String extid);
    boolean existsByUniqueId(String uniqueId);
    Optional<AiPromptEnvelopeDb> findByUniqueId(String uniqueId);

    Optional<AiPromptEnvelopeDb> findByUniqueIdAndLifecycle(String uniqueId, AiLifecycle lifecycle);

    default List<AiPromptEnvelopeDb> findAllActive() {
        return findByActive(ActiveEnum.ACTIVE);
    }
}
