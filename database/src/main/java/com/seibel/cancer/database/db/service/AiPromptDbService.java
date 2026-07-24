package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.domain.AiPrompt;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.domain.enums.ai.AiLifecycle;
import com.seibel.cancer.database.db.entity.ai.AiPromptDb;
import com.seibel.cancer.database.db.mapper.AiPromptMapper;
import com.seibel.cancer.database.db.entity.ai.AiPromptEnvelopeDb;
import com.seibel.cancer.database.db.repository.AiPromptEnvelopeRepository;
import com.seibel.cancer.database.db.repository.AiPromptRepository;
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
public class AiPromptDbService extends BaseDbService {

    private final AiPromptRepository repository;
    private final AiPromptMapper mapper;
    private final AiPromptEnvelopeRepository envelopeRepository;

    public AiPromptDbService(AiPromptRepository repository, AiPromptMapper mapper, AiPromptEnvelopeRepository envelopeRepository) {
        super("AiPromptDb");
        this.repository = repository;
        this.mapper = mapper;
        this.envelopeRepository = envelopeRepository;
    }

    public AiPrompt create(String uniqueId, @NonNull String name, String description,
                           @NonNull String provider, String model,
                           String version, @NonNull String content,
                           String resultFormat, String exampleInput, String exampleOutput,
                           String jsonVariables, String jsonVariablesTest, String envelopeExtid,
                           AiLifecycle lifecycle)
            throws DatabaseFailureException {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            AiPromptDb record = new AiPromptDb();
            record.setExtid(extid);
            record.setUniqueId(uniqueId);
            record.setName(name);
            record.setDescription(description);
            record.setProvider(provider);
            record.setModel(model);
            record.setVersion(version);
            record.setContent(content);
            record.setResultFormat(resultFormat);
            record.setExampleInput(exampleInput);
            record.setExampleOutput(exampleOutput);
            record.setJsonVariables(jsonVariables);
            record.setJsonVariablesTest(jsonVariablesTest);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);
            record.setLifecycle(lifecycle);
            if (envelopeExtid != null) {
                AiPromptEnvelopeDb envelope = envelopeRepository.findByExtid(envelopeExtid).orElse(null);
                record.setEnvelope(envelope);
            }

            AiPromptDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            AiPrompt result = mapper.toModel(saved);
            result.setEnvelopeExtid(saved.getEnvelope() != null ? saved.getEnvelope().getExtid() : null);
            result.setEnvelopeName(saved.getEnvelope() != null ? saved.getEnvelope().getName() : null);
            return result;

        } catch (Exception e) {
            handleException("create", extid, e);
            return null;
        }
    }

    public AiPrompt update(@NonNull String extid, String uniqueId, String name, String description,
                           String provider, String model,
                           String version, String content,
                           String resultFormat, String exampleInput, String exampleOutput,
                           String jsonVariables, String jsonVariablesTest, String envelopeExtid,
                           AiLifecycle lifecycle)
            throws DatabaseFailureException {

        AiPromptDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new DatabaseFailureException(getFoundFailureMessage(extid)));

        try {
            if (uniqueId != null) record.setUniqueId(uniqueId);
            if (name != null) record.setName(name);
            if (description != null) record.setDescription(description);
            if (provider != null) record.setProvider(provider);
            if (model != null) record.setModel(model);
            if (version != null) record.setVersion(version);
            if (content != null) record.setContent(content);
            if (resultFormat != null) record.setResultFormat(resultFormat);
            if (exampleInput != null) record.setExampleInput(exampleInput);
            if (exampleOutput != null) record.setExampleOutput(exampleOutput);
            if (jsonVariables != null) record.setJsonVariables(jsonVariables);
            if (jsonVariablesTest != null) record.setJsonVariablesTest(jsonVariablesTest);
            if (envelopeExtid != null) {
                AiPromptEnvelopeDb envelope = envelopeRepository.findByExtid(envelopeExtid).orElse(null);
                record.setEnvelope(envelope);
            }
            if (lifecycle != null) record.setLifecycle(lifecycle);
            record.setUpdatedAt(LocalDateTime.now());

            AiPromptDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            AiPrompt result = mapper.toModel(saved);
            result.setEnvelopeExtid(saved.getEnvelope() != null ? saved.getEnvelope().getExtid() : null);
            result.setEnvelopeName(saved.getEnvelope() != null ? saved.getEnvelope().getName() : null);
            return result;

        } catch (Exception e) {
            handleException("update", extid, e);
            return null;
        }
    }

    public boolean delete(@NonNull String extid)
            throws DatabaseFailureException {

        AiPromptDb record = repository.findByExtid(extid)
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
    public AiPrompt promote(@NonNull String extid) throws DatabaseFailureException {
        AiPromptDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new DatabaseFailureException(getFoundFailureMessage(extid)));

        if (record.getLifecycle() == AiLifecycle.DISCREDITED) {
            throw new DatabaseFailureException("Cannot promote a DISCREDITED AiPrompt.");
        }

        if (record.getEnvelope() == null || record.getEnvelope().getLifecycle() != AiLifecycle.IN_PRODUCTION) {
            throw new DatabaseFailureException("Cannot promote AiPrompt: parent Envelope is not IN_PRODUCTION.");
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
        AiPromptDb saved = repository.save(record);
        log.info("promote(): extid={}", extid);
        AiPrompt result = mapper.toModel(saved);
        result.setEnvelopeExtid(saved.getEnvelope() != null ? saved.getEnvelope().getExtid() : null);
        result.setEnvelopeName(saved.getEnvelope() != null ? saved.getEnvelope().getName() : null);
        return result;
    }

    public AiPrompt findByUniqueId(@NonNull String uniqueId) throws DatabaseFailureException {
        AiPromptDb record = repository.findByUniqueId(uniqueId)
                .orElseThrow(() -> new DatabaseFailureException(getFoundFailureMessage(uniqueId)));
        log.info(getFoundMessage(uniqueId));
        AiPrompt model = mapper.toModel(record);
        model.setEnvelopeExtid(record.getEnvelope() != null ? record.getEnvelope().getExtid() : null);
        model.setEnvelopeName(record.getEnvelope() != null ? record.getEnvelope().getName() : null);
        return model;
    }

    public AiPrompt findByExtid(@NonNull String extid) throws DatabaseFailureException {
        AiPromptDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new DatabaseFailureException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        AiPrompt model = mapper.toModel(record);
        model.setEnvelopeExtid(record.getEnvelope() != null ? record.getEnvelope().getExtid() : null);
        model.setEnvelopeName(record.getEnvelope() != null ? record.getEnvelope().getName() : null);
        return model;
    }

    public List<AiPrompt> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public List<AiPrompt> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    private List<AiPrompt> findAndLog(List<AiPromptDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return records.stream().map(record -> {
            AiPrompt model = mapper.toModel(record);
            model.setEnvelopeExtid(record.getEnvelope() != null ? record.getEnvelope().getExtid() : null);
            model.setEnvelopeName(record.getEnvelope() != null ? record.getEnvelope().getName() : null);
            return model;
        }).toList();
    }
}
