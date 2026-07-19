package com.seibel.jobs.database.db.service;

import com.seibel.jobs.common.domain.Skill;
import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.common.exceptions.ServiceException;
import com.seibel.jobs.database.db.entity.SkillDb;
import com.seibel.jobs.database.db.mapper.SkillMapper;
import com.seibel.jobs.database.db.repository.SkillRepository;
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
public class SkillDbService extends BaseDbService {

    private final SkillRepository repository;
    private final SkillMapper mapper;

    public SkillDbService(SkillRepository repository, SkillMapper mapper) {
        super("SkillDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public Skill create(@NonNull Skill item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            SkillDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            SkillDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null;
        }
    }

    public Skill create(@NonNull String name) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            SkillDb record = new SkillDb();
            record.setExtid(extid);
            record.setName(name);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            SkillDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public Skill update(@NonNull String extid, String name) {

        SkillDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (name != null) record.setName(name);
            record.setUpdatedAt(LocalDateTime.now());

            SkillDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null;
        }
    }

    public boolean delete(@NonNull String extid) {

        SkillDb record = repository.findByExtid(extid)
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

    public Skill findByExtid(@NonNull String extid) {
        SkillDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<Skill> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<Skill> findAll(Pageable pageable) {
        Page<SkillDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<Skill> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<Skill> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<SkillDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    private List<Skill> findAndLog(List<SkillDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
