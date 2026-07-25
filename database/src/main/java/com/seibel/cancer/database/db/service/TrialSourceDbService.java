package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.TrialSource;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.TrialSourceDb;
import com.seibel.cancer.database.db.mapper.TrialSourceMapper;
import com.seibel.cancer.database.db.repository.TrialSourceRepository;
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
public class TrialSourceDbService extends BaseDbService {

    private final TrialSourceRepository repository;
    private final TrialSourceMapper mapper;

    public TrialSourceDbService(TrialSourceRepository repository, TrialSourceMapper mapper) {
        super("TrialSourceDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public TrialSource create(@NonNull TrialSource item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            TrialSourceDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            TrialSourceDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null;
        }
    }

    public TrialSource create(@NonNull String code, @NonNull String name, @NonNull String baseUrl) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            TrialSourceDb record = new TrialSourceDb();
            record.setExtid(extid);
            record.setCode(code);
            record.setName(name);
            record.setBaseUrl(baseUrl);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            TrialSourceDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public TrialSource update(@NonNull String extid, String code, String name, String baseUrl) {

        TrialSourceDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (code != null) record.setCode(code);
            if (name != null) record.setName(name);
            if (baseUrl != null) record.setBaseUrl(baseUrl);
            record.setUpdatedAt(LocalDateTime.now());

            TrialSourceDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null;
        }
    }

    public boolean delete(@NonNull String extid) {

        TrialSourceDb record = repository.findByExtid(extid)
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

    public TrialSource findByExtid(@NonNull String extid) {
        TrialSourceDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<TrialSource> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<TrialSource> findAll(Pageable pageable) {
        Page<TrialSourceDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<TrialSource> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<TrialSource> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<TrialSourceDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    private List<TrialSource> findAndLog(List<TrialSourceDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
