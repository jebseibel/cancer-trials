package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.AiPromptGang;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.enums.ai.AiLifecycle;
import com.seibel.cancer.database.db.entity.ai.AiPromptGangDb;
import com.seibel.cancer.database.db.mapper.AiPromptGangMapper;
import com.seibel.cancer.database.db.repository.AiPromptGangRepository;
import com.seibel.cancer.database.exceptions.DatabaseFailureException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class AiPromptGangDbService extends BaseDbService {

    private final AiPromptGangRepository repository;
    private final AiPromptGangMapper mapper;

    public AiPromptGangDbService(AiPromptGangRepository repository, AiPromptGangMapper mapper) {
        super("AiPromptGangDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public AiPromptGang create(String uniqueId, @NonNull String name, String description,
                               @NonNull String persona, String gang, String subgang,
                               String tone, String promptSequence, String prohibitions,
                               String decisionPrinciples, Integer memoryDepth, String version,
                               AiLifecycle lifecycle)
            throws DatabaseFailureException {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            AiPromptGangDb record = new AiPromptGangDb();
            record.setExtid(extid);
            record.setUniqueId(uniqueId);
            record.setName(name);
            record.setDescription(description);
            record.setPersona(persona);
            record.setGang(gang);
            record.setSubgang(subgang);
            record.setTone(tone);
            record.setPromptSequence(promptSequence);
            record.setProhibitions(prohibitions);
            record.setDecisionPrinciples(decisionPrinciples);
            record.setMemoryDepth(memoryDepth);
            record.setVersion(version);
            record.setLifecycle(lifecycle);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            AiPromptGangDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null;
        }
    }

    public AiPromptGang update(@NonNull String extid, String uniqueId, String name, String description,
                               String persona, String gang, String subgang,
                               String tone, String promptSequence, String prohibitions,
                               String decisionPrinciples, Integer memoryDepth, String version,
                               AiLifecycle lifecycle)
            throws DatabaseFailureException {

        AiPromptGangDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new DatabaseFailureException(getFoundFailureMessage(extid)));

        try {
            if (uniqueId != null) record.setUniqueId(uniqueId);
            if (name != null) record.setName(name);
            if (description != null) record.setDescription(description);
            if (persona != null) record.setPersona(persona);
            if (gang != null) record.setGang(gang);
            if (subgang != null) record.setSubgang(subgang);
            if (tone != null) record.setTone(tone);
            if (promptSequence != null) record.setPromptSequence(promptSequence);
            if (prohibitions != null) record.setProhibitions(prohibitions);
            if (decisionPrinciples != null) record.setDecisionPrinciples(decisionPrinciples);
            if (memoryDepth != null) record.setMemoryDepth(memoryDepth);
            if (version != null) record.setVersion(version);
            if (lifecycle != null) record.setLifecycle(lifecycle);
            record.setUpdatedAt(LocalDateTime.now());

            AiPromptGangDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null;
        }
    }

    public boolean delete(@NonNull String extid)
            throws DatabaseFailureException {

        AiPromptGangDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new DatabaseFailureException(getFoundFailureMessage(extid)));

        try {
            record.setDeletedAt(LocalDateTime.now());
            record.setActive(ActiveEnum.INACTIVE);

            repository.save(record);
            log.info(getDeletedMessage(extid));
            return true;

        } catch (Exception e) {
            handleException("delete", extid, e);
            return false;
        }
    }

    @Transactional
    public AiPromptGang promote(@NonNull String extid) throws DatabaseFailureException {
        AiPromptGangDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new DatabaseFailureException(getFoundFailureMessage(extid)));

        if (record.getLifecycle() == AiLifecycle.DISCREDITED) {
            throw new DatabaseFailureException("Cannot promote a DISCREDITED AiPromptGang.");
        }

        repository.findByUniqueIdAndLifecycle(record.getUniqueId(), AiLifecycle.IN_PRODUCTION)
                .filter(current -> !current.getExtid().equals(extid))
                .ifPresent(current -> {
                    current.setLifecycle(AiLifecycle.RETIRED);
                    current.setUpdatedAt(LocalDateTime.now());
                    repository.save(current);
                });

        record.setLifecycle(AiLifecycle.IN_PRODUCTION);
        record.setUpdatedAt(LocalDateTime.now());
        AiPromptGangDb saved = repository.save(record);
        log.info("promote(): extid={}", extid);
        return mapper.toModel(saved);
    }

    public AiPromptGang findByExtid(@NonNull String extid) throws DatabaseFailureException {
        AiPromptGangDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new DatabaseFailureException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<AiPromptGang> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public List<AiPromptGang> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    private List<AiPromptGang> findAndLog(List<AiPromptGangDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }
}
