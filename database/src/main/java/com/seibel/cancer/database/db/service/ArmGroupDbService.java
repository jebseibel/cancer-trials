package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.ArmGroup;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.ArmGroupDb;
import com.seibel.cancer.database.db.mapper.ArmGroupMapper;
import com.seibel.cancer.database.db.repository.ArmGroupRepository;
import com.seibel.cancer.common.exceptions.ServiceException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ArmGroupDbService extends BaseDbService {

    private final ArmGroupRepository repository;
    private final ArmGroupMapper mapper;

    public ArmGroupDbService(ArmGroupRepository repository, ArmGroupMapper mapper) {
        super("ArmGroupDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public ArmGroup create(@NonNull ArmGroup item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            ArmGroupDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            ArmGroupDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null;
        }
    }

    public ArmGroup create(@NonNull Long trialId, @NonNull String label, String type, String description) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            ArmGroupDb record = new ArmGroupDb();
            record.setExtid(extid);
            record.setTrialId(trialId);
            record.setLabel(label);
            record.setType(type);
            record.setDescription(description);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            ArmGroupDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public ArmGroup update(@NonNull String extid, Long trialId, String label, String type, String description) {

        ArmGroupDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (trialId != null) record.setTrialId(trialId);
            if (label != null) record.setLabel(label);
            if (type != null) record.setType(type);
            if (description != null) record.setDescription(description);
            record.setUpdatedAt(LocalDateTime.now());

            ArmGroupDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null;
        }
    }

    public boolean delete(@NonNull String extid) {

        ArmGroupDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            record.setDeletedAt(LocalDateTime.now());
            record.setActive(ActiveEnum.INACTIVE);

            repository.save(record);
            log.info(getDeletedMessage(extid));
            return true;

        } catch (Exception e) {
            handleException("delete", extid, e);
            return false; // unreachable
        }
    }

    public ArmGroup findByExtid(@NonNull String extid) {
        ArmGroupDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<ArmGroup> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public List<ArmGroup> findByTrialId(@NonNull Long trialId) {
        return findAndLog(repository.findByTrialId(trialId), String.format("trialId (%d)", trialId));
    }

    public Page<ArmGroup> findAll(Pageable pageable) {
        Page<ArmGroupDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<ArmGroup> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<ArmGroup> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<ArmGroupDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    private List<ArmGroup> findAndLog(List<ArmGroupDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
