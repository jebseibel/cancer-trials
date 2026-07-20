package com.seibel.jobhunting.service;

import com.seibel.jobhunting.common.domain.JobPosting;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.common.exceptions.ResourceNotFoundException;
import com.seibel.jobhunting.common.exceptions.ServiceException;
import com.seibel.jobhunting.database.db.service.JobPostingDbService;
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
public class JobPostingService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("title", "postedAt", "createdAt", "updatedAt");

    private final JobPostingDbService dbService;

    public JobPostingService(JobPostingDbService dbService) {
        super(JobPosting.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public JobPosting create(JobPosting item) {
        requireNonNull(item, "JobPosting");
        log.info("create(): {}", item);

        try {
            return dbService.create(item.getTitle(), item.getCompanyId(), item.getDescription(), item.getCity(), item.getState(), item.getCountry(),
                    item.getWorkMode(), item.getSalaryMin(), item.getSalaryMax(), item.getSalaryCurrency(),
                    item.getSource(), item.getSourceUrl(), item.getPostedAt(), item.getStatus(), item.getNotes());
        } catch (Exception e) {
            log.error("Failed to create job posting: {}", item.getTitle(), e);
            throw new ServiceException("Unable to create job posting", e);
        }
    }

    @Transactional
    public JobPosting update(String extid, JobPosting item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "JobPosting");
        log.info("update(): extid={}, {}", extid, item);

        try {
            JobPosting updated = dbService.update(extid, item.getTitle(), item.getCompanyId(), item.getDescription(), item.getCity(), item.getState(), item.getCountry(),
                    item.getWorkMode(), item.getSalaryMin(), item.getSalaryMax(), item.getSalaryCurrency(),
                    item.getSource(), item.getSourceUrl(), item.getPostedAt(), item.getStatus(), item.getNotes());
            if (updated == null) {
                throw new ResourceNotFoundException("JobPosting", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update job posting: {}", extid, e);
            throw new ServiceException("Unable to update job posting", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete job posting: {}", extid, e);
            throw new ServiceException("Unable to delete job posting", e);
        }
    }

    public JobPosting findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            JobPosting jobPosting = dbService.findByExtid(extid);
            if (jobPosting == null) {
                throw new ResourceNotFoundException("JobPosting", extid);
            }
            return jobPosting;
        } catch (Exception e) {
            log.error("Failed to retrieve job posting: {}", extid, e);
            throw new ServiceException("Unable to retrieve job posting", e);
        }
    }

    public List<JobPosting> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all job postings", e);
            throw new ServiceException("Unable to retrieve job postings", e);
        }
    }

    public Page<JobPosting> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve job postings (paged)", e);
            throw new ServiceException("Unable to retrieve job postings", e);
        }
    }

    public List<JobPosting> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve job postings by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve job postings", e);
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
            // If client requested only invalid fields, fall back to postedAt DESC
            safeSort = Sort.by(Sort.Order.desc("postedAt"));
        }
        return PageRequest.of(pageable.getPageNumber(), size, safeSort);
    }
}
