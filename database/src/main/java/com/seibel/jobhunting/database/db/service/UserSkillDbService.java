package com.seibel.jobhunting.database.db.service;

import com.seibel.jobhunting.common.domain.UserSkill;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.common.exceptions.ServiceException;
import com.seibel.jobhunting.database.db.entity.UserSkillDb;
import com.seibel.jobhunting.database.db.mapper.UserSkillMapper;
import com.seibel.jobhunting.database.db.repository.UserSkillRepository;
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
public class UserSkillDbService extends BaseDbService {

    private final UserSkillRepository repository;
    private final UserSkillMapper mapper;

    public UserSkillDbService(UserSkillRepository repository, UserSkillMapper mapper) {
        super("UserSkillDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public UserSkill create(@NonNull UserSkill item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            UserSkillDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            UserSkillDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null;
        }
    }

    public UserSkill create(@NonNull Long userId, @NonNull Long skillId) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            UserSkillDb record = new UserSkillDb();
            record.setExtid(extid);
            record.setUserId(userId);
            record.setSkillId(skillId);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            UserSkillDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public UserSkill update(@NonNull String extid, Long userId, Long skillId) {

        UserSkillDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (userId != null) record.setUserId(userId);
            if (skillId != null) record.setSkillId(skillId);
            record.setUpdatedAt(LocalDateTime.now());

            UserSkillDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null;
        }
    }

    public boolean delete(@NonNull String extid) {

        UserSkillDb record = repository.findByExtid(extid)
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

    public UserSkill findByExtid(@NonNull String extid) {
        UserSkillDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<UserSkill> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<UserSkill> findAll(Pageable pageable) {
        Page<UserSkillDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<UserSkill> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<UserSkill> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<UserSkillDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<UserSkill> findByUserId(@NonNull Long userId) {
        return findAndLog(repository.findByUserId(userId), String.format("userId (%s)", userId));
    }

    public List<UserSkill> findBySkillId(@NonNull Long skillId) {
        return findAndLog(repository.findBySkillId(skillId), String.format("skillId (%s)", skillId));
    }

    private List<UserSkill> findAndLog(List<UserSkillDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
