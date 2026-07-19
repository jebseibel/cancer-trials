package com.seibel.jobs.database.db.service;

import com.seibel.jobs.common.domain.FriendSkill;
import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.common.exceptions.ServiceException;
import com.seibel.jobs.database.db.entity.FriendSkillDb;
import com.seibel.jobs.database.db.mapper.FriendSkillMapper;
import com.seibel.jobs.database.db.repository.FriendSkillRepository;
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
public class FriendSkillDbService extends BaseDbService {

    private final FriendSkillRepository repository;
    private final FriendSkillMapper mapper;

    public FriendSkillDbService(FriendSkillRepository repository, FriendSkillMapper mapper) {
        super("FriendSkillDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public FriendSkill create(@NonNull FriendSkill item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            FriendSkillDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            FriendSkillDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null;
        }
    }

    public FriendSkill create(@NonNull Long friendId, @NonNull Long skillId) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            FriendSkillDb record = new FriendSkillDb();
            record.setExtid(extid);
            record.setFriendId(friendId);
            record.setSkillId(skillId);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            FriendSkillDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public FriendSkill update(@NonNull String extid, Long friendId, Long skillId) {

        FriendSkillDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (friendId != null) record.setFriendId(friendId);
            if (skillId != null) record.setSkillId(skillId);
            record.setUpdatedAt(LocalDateTime.now());

            FriendSkillDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null;
        }
    }

    public boolean delete(@NonNull String extid) {

        FriendSkillDb record = repository.findByExtid(extid)
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

    public FriendSkill findByExtid(@NonNull String extid) {
        FriendSkillDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<FriendSkill> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<FriendSkill> findAll(Pageable pageable) {
        Page<FriendSkillDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<FriendSkill> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<FriendSkill> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<FriendSkillDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<FriendSkill> findByFriendId(@NonNull Long friendId) {
        return findAndLog(repository.findByFriendId(friendId), String.format("friendId (%s)", friendId));
    }

    public List<FriendSkill> findBySkillId(@NonNull Long skillId) {
        return findAndLog(repository.findBySkillId(skillId), String.format("skillId (%s)", skillId));
    }

    private List<FriendSkill> findAndLog(List<FriendSkillDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
