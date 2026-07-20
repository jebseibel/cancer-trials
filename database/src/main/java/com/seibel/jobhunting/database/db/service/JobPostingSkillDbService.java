package com.seibel.jobhunting.database.db.service;

import com.seibel.jobhunting.common.domain.JobPostingSkill;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.common.exceptions.ServiceException;
import com.seibel.jobhunting.database.db.entity.JobPostingSkillDb;
import com.seibel.jobhunting.database.db.mapper.JobPostingSkillMapper;
import com.seibel.jobhunting.database.db.repository.JobPostingSkillRepository;
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
public class JobPostingSkillDbService extends BaseDbService {

    private final JobPostingSkillRepository repository;
    private final JobPostingSkillMapper mapper;

    public JobPostingSkillDbService(JobPostingSkillRepository repository, JobPostingSkillMapper mapper) {
        super("JobPostingSkillDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public JobPostingSkill create(@NonNull JobPostingSkill item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            JobPostingSkillDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            JobPostingSkillDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null;
        }
    }

    public JobPostingSkill create(@NonNull Long jobPostingId, @NonNull Long skillId) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            JobPostingSkillDb record = new JobPostingSkillDb();
            record.setExtid(extid);
            record.setJobPostingId(jobPostingId);
            record.setSkillId(skillId);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            JobPostingSkillDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public JobPostingSkill update(@NonNull String extid, Long jobPostingId, Long skillId) {

        JobPostingSkillDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (jobPostingId != null) record.setJobPostingId(jobPostingId);
            if (skillId != null) record.setSkillId(skillId);
            record.setUpdatedAt(LocalDateTime.now());

            JobPostingSkillDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null;
        }
    }

    public boolean delete(@NonNull String extid) {

        JobPostingSkillDb record = repository.findByExtid(extid)
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

    public JobPostingSkill findByExtid(@NonNull String extid) {
        JobPostingSkillDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<JobPostingSkill> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<JobPostingSkill> findAll(Pageable pageable) {
        Page<JobPostingSkillDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<JobPostingSkill> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<JobPostingSkill> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<JobPostingSkillDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<JobPostingSkill> findByJobPostingId(@NonNull Long jobPostingId) {
        return findAndLog(repository.findByJobPostingId(jobPostingId), String.format("jobPostingId (%s)", jobPostingId));
    }

    public List<JobPostingSkill> findBySkillId(@NonNull Long skillId) {
        return findAndLog(repository.findBySkillId(skillId), String.format("skillId (%s)", skillId));
    }

    private List<JobPostingSkill> findAndLog(List<JobPostingSkillDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
