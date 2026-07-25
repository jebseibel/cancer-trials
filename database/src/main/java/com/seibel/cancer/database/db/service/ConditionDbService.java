package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.Condition;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.ConditionDb;
import com.seibel.cancer.database.db.mapper.ConditionMapper;
import com.seibel.cancer.database.db.repository.ConditionRepository;
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
public class ConditionDbService extends BaseDbService {

    private final ConditionRepository repository;
    private final ConditionMapper mapper;

    public ConditionDbService(ConditionRepository repository, ConditionMapper mapper) {
        super("ConditionDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public Condition create(@NonNull Condition item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            ConditionDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            ConditionDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public Condition create(@NonNull String name) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            ConditionDb record = new ConditionDb();
            record.setExtid(extid);
            record.setName(name);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            ConditionDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public Condition update(@NonNull String extid, String name) {

        ConditionDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (name != null) record.setName(name);
            record.setUpdatedAt(LocalDateTime.now());

            ConditionDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null; // unreachable
        }
    }

    public boolean delete(@NonNull String extid) {

        ConditionDb record = repository.findByExtid(extid)
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

    public Condition findByExtid(@NonNull String extid) {
        ConditionDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<Condition> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<Condition> findAll(Pageable pageable) {
        Page<ConditionDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<Condition> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<Condition> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<ConditionDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    private List<Condition> findAndLog(List<ConditionDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
