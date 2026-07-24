package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.AiPromptEnvelope;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.enums.ai.AiLifecycle;
import com.seibel.cancer.database.db.entity.ai.AiPromptEnvelopeDb;
import com.seibel.cancer.database.db.entity.ai.AiPromptGangDb;
import com.seibel.cancer.database.db.mapper.AiPromptEnvelopeMapper;
import com.seibel.cancer.database.db.repository.AiPromptEnvelopeRepository;
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
public class AiPromptEnvelopeDbService extends BaseDbService {

    private final AiPromptEnvelopeRepository repository;
    private final AiPromptEnvelopeMapper mapper;
    private final AiPromptGangRepository gangRepository;

    public AiPromptEnvelopeDbService(AiPromptEnvelopeRepository repository, AiPromptEnvelopeMapper mapper, AiPromptGangRepository gangRepository) {
        super("AiPromptEnvelopeDb");
        this.repository = repository;
        this.mapper = mapper;
        this.gangRepository = gangRepository;
    }

    public AiPromptEnvelope create(String uniqueId, @NonNull String name, String description,
                                   String category, String subCategory,
                                   String resultType, String subPersona,
                                   String goal, String decisionPrinciples,
                                   String toolExclusions, @NonNull AiLifecycle lifecycle,
                                   Double temperature, Integer maxRetries,
                                   Integer timeoutSeconds, String version)
            throws DatabaseFailureException {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            AiPromptEnvelopeDb record = new AiPromptEnvelopeDb();
            record.setExtid(extid);
            record.setUniqueId(uniqueId);
            record.setName(name);
            record.setDescription(description);
            record.setCategory(category);
            record.setSubCategory(subCategory);
            record.setResultType(resultType);
            record.setSubPersona(subPersona);
            record.setGoal(goal);
            record.setDecisionPrinciples(decisionPrinciples);
            record.setToolExclusions(toolExclusions);
            record.setLifecycle(lifecycle);
            record.setTemperature(temperature);
            record.setMaxRetries(maxRetries);
            record.setTimeoutSeconds(timeoutSeconds);
            record.setVersion(version);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            AiPromptEnvelopeDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            AiPromptEnvelope result = mapper.toModel(saved);
            result.setGangExtid(saved.getGang() != null ? saved.getGang().getExtid() : null);
            result.setGangName(saved.getGang() != null ? saved.getGang().getName() : null);
            return result;

        } catch (Exception e) {
            handleException("create", extid, e);
            return null;
        }
    }

    public AiPromptEnvelope update(@NonNull String extid, String uniqueId, String name, String description,
                                   String category, String subCategory,
                                   String resultType, String subPersona,
                                   String goal, String decisionPrinciples,
                                   String toolExclusions, AiLifecycle lifecycle,
                                   Double temperature, Integer maxRetries,
                                   Integer timeoutSeconds, String version,
                                   String gangExtid)
            throws DatabaseFailureException {

        AiPromptEnvelopeDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new DatabaseFailureException(getFoundFailureMessage(extid)));

        try {
            if (uniqueId != null) record.setUniqueId(uniqueId);
            if (name != null) record.setName(name);
            if (description != null) record.setDescription(description);
            if (category != null) record.setCategory(category);
            if (subCategory != null) record.setSubCategory(subCategory);
            if (resultType != null) record.setResultType(resultType);
            if (subPersona != null) record.setSubPersona(subPersona);
            if (goal != null) record.setGoal(goal);
            if (decisionPrinciples != null) record.setDecisionPrinciples(decisionPrinciples);
            if (toolExclusions != null) record.setToolExclusions(toolExclusions);
            if (lifecycle != null) record.setLifecycle(lifecycle);
            if (temperature != null) record.setTemperature(temperature);
            if (maxRetries != null) record.setMaxRetries(maxRetries);
            if (timeoutSeconds != null) record.setTimeoutSeconds(timeoutSeconds);
            if (version != null) record.setVersion(version);
            if (gangExtid != null) {
                AiPromptGangDb gang = gangRepository.findByExtid(gangExtid).orElse(null);
                record.setGang(gang);
            }
            record.setUpdatedAt(LocalDateTime.now());

            AiPromptEnvelopeDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            AiPromptEnvelope result = mapper.toModel(saved);
            result.setGangExtid(saved.getGang() != null ? saved.getGang().getExtid() : null);
            result.setGangName(saved.getGang() != null ? saved.getGang().getName() : null);
            return result;

        } catch (Exception e) {
            handleException("update", extid, e);
            return null;
        }
    }

    public boolean delete(@NonNull String extid)
            throws DatabaseFailureException {

        AiPromptEnvelopeDb record = repository.findByExtid(extid)
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
    public AiPromptEnvelope promote(@NonNull String extid) throws DatabaseFailureException {
        AiPromptEnvelopeDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new DatabaseFailureException(getFoundFailureMessage(extid)));

        if (record.getLifecycle() == AiLifecycle.DISCREDITED) {
            throw new DatabaseFailureException("Cannot promote a DISCREDITED AiPromptEnvelope.");
        }

        if (record.getGang() == null || record.getGang().getLifecycle() != AiLifecycle.IN_PRODUCTION) {
            throw new DatabaseFailureException("Cannot promote AiPromptEnvelope: parent Gang is not IN_PRODUCTION.");
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
        AiPromptEnvelopeDb saved = repository.save(record);
        log.info("promote(): extid={}", extid);
        AiPromptEnvelope result = mapper.toModel(saved);
        result.setGangExtid(saved.getGang() != null ? saved.getGang().getExtid() : null);
        result.setGangName(saved.getGang() != null ? saved.getGang().getName() : null);
        return result;
    }

    public AiPromptEnvelope findByExtid(@NonNull String extid) throws DatabaseFailureException {
        AiPromptEnvelopeDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new DatabaseFailureException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        AiPromptEnvelope model = mapper.toModel(record);
        model.setGangExtid(record.getGang() != null ? record.getGang().getExtid() : null);
        model.setGangName(record.getGang() != null ? record.getGang().getName() : null);
        return model;
    }

    public List<AiPromptEnvelope> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public List<AiPromptEnvelope> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    private List<AiPromptEnvelope> findAndLog(List<AiPromptEnvelopeDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return records.stream().map(record -> {
            AiPromptEnvelope model = mapper.toModel(record);
            model.setGangExtid(record.getGang() != null ? record.getGang().getExtid() : null);
            model.setGangName(record.getGang() != null ? record.getGang().getName() : null);
            return model;
        }).toList();
    }
}
