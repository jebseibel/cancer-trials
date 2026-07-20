package com.seibel.jobhunting.service;

import com.seibel.jobhunting.common.domain.JobPostingSkill;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.common.exceptions.ResourceNotFoundException;
import com.seibel.jobhunting.common.exceptions.ServiceException;
import com.seibel.jobhunting.database.db.service.JobPostingSkillDbService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@Transactional(readOnly = true)
public class JobPostingSkillService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "updatedAt");

    private final JobPostingSkillDbService dbService;

    public JobPostingSkillService(JobPostingSkillDbService dbService) {
        super(JobPostingSkill.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public JobPostingSkill create(JobPostingSkill item) {
        requireNonNull(item, "JobPostingSkill");
        log.info("create(): {}", item);

        try {
            return dbService.create(item.getJobPostingId(), item.getSkillId());
        } catch (Exception e) {
            log.error("Failed to create job posting skill: {}", item, e);
            throw new ServiceException("Unable to create job posting skill", e);
        }
    }

    @Transactional
    public JobPostingSkill update(String extid, JobPostingSkill item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "JobPostingSkill");
        log.info("update(): extid={}, {}", extid, item);

        try {
            JobPostingSkill updated = dbService.update(extid, item.getJobPostingId(), item.getSkillId());
            if (updated == null) {
                throw new ResourceNotFoundException("JobPostingSkill", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update job posting skill: {}", extid, e);
            throw new ServiceException("Unable to update job posting skill", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete job posting skill: {}", extid, e);
            throw new ServiceException("Unable to delete job posting skill", e);
        }
    }

    public JobPostingSkill findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            JobPostingSkill jobPostingSkill = dbService.findByExtid(extid);
            if (jobPostingSkill == null) {
                throw new ResourceNotFoundException("JobPostingSkill", extid);
            }
            return jobPostingSkill;
        } catch (Exception e) {
            log.error("Failed to retrieve job posting skill: {}", extid, e);
            throw new ServiceException("Unable to retrieve job posting skill", e);
        }
    }

    public List<JobPostingSkill> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all job posting skills", e);
            throw new ServiceException("Unable to retrieve job posting skills", e);
        }
    }

    public Page<JobPostingSkill> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve job posting skills (paged)", e);
            throw new ServiceException("Unable to retrieve job posting skills", e);
        }
    }

    public List<JobPostingSkill> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve job posting skills by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve job posting skills", e);
        }
    }

    public List<JobPostingSkill> findByJobPostingId(Long jobPostingId) {
        requireNonNull(jobPostingId, "jobPostingId");
        log.info("findByJobPostingId(): jobPostingId={}", jobPostingId);

        try {
            return dbService.findByJobPostingId(jobPostingId);
        } catch (Exception e) {
            log.error("Failed to retrieve job posting skills by jobPostingId: {}", jobPostingId, e);
            throw new ServiceException("Unable to retrieve job posting skills", e);
        }
    }

    public List<JobPostingSkill> findBySkillId(Long skillId) {
        requireNonNull(skillId, "skillId");
        log.info("findBySkillId(): skillId={}", skillId);

        try {
            return dbService.findBySkillId(skillId);
        } catch (Exception e) {
            log.error("Failed to retrieve job posting skills by skillId: {}", skillId, e);
            throw new ServiceException("Unable to retrieve job posting skills", e);
        }
    }

    private Pageable enforceCapsAndWhitelist(Pageable pageable) {
        int size = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);
        Sort safeSort = pageable.getSort().isUnsorted() ? Sort.unsorted() :
                pageable.getSort().stream()
                        .filter(order -> ALLOWED_SORT_FIELDS.contains(order.getProperty()))
                        .collect(() -> Sort.unsorted(),
                                (acc, order) -> acc.and(Sort.by(order.getDirection(), order.getProperty())),
                                Sort::and);
        if (safeSort.isUnsorted() && pageable.getSort().isSorted()) {
            // If client requested only invalid fields, fall back to createdAt DESC
            safeSort = Sort.by(Sort.Order.desc("createdAt"));
        }
        return PageRequest.of(pageable.getPageNumber(), size, safeSort);
    }
}
