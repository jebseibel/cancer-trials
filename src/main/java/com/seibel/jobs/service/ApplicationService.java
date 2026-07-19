package com.seibel.jobs.service;

import com.seibel.jobs.common.domain.Application;
import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.common.exceptions.ResourceNotFoundException;
import com.seibel.jobs.common.exceptions.ServiceException;
import com.seibel.jobs.database.db.service.ApplicationDbService;
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
public class ApplicationService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("dateApplied", "createdAt", "updatedAt");

    private final ApplicationDbService dbService;

    public ApplicationService(ApplicationDbService dbService) {
        super(Application.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public Application create(Application item) {
        requireNonNull(item, "Application");
        log.info("create(): {}", item);

        try {
            return dbService.create(item.getJobPostingId(), item.getDateApplied(), item.getResumeVersion(), item.getApplicationStatus(), item.getNotes());
        } catch (Exception e) {
            log.error("Failed to create application: {}", item.getJobPostingId(), e);
            throw new ServiceException("Unable to create application", e);
        }
    }

    @Transactional
    public Application update(String extid, Application item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "Application");
        log.info("update(): extid={}, {}", extid, item);

        try {
            Application updated = dbService.update(extid, item.getJobPostingId(), item.getDateApplied(), item.getResumeVersion(), item.getApplicationStatus(), item.getNotes());
            if (updated == null) {
                throw new ResourceNotFoundException("Application", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update application: {}", extid, e);
            throw new ServiceException("Unable to update application", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete application: {}", extid, e);
            throw new ServiceException("Unable to delete application", e);
        }
    }

    public Application findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            Application application = dbService.findByExtid(extid);
            if (application == null) {
                throw new ResourceNotFoundException("Application", extid);
            }
            return application;
        } catch (Exception e) {
            log.error("Failed to retrieve application: {}", extid, e);
            throw new ServiceException("Unable to retrieve application", e);
        }
    }

    public List<Application> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all applications", e);
            throw new ServiceException("Unable to retrieve applications", e);
        }
    }

    public Page<Application> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve applications (paged)", e);
            throw new ServiceException("Unable to retrieve applications", e);
        }
    }

    public List<Application> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve applications by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve applications", e);
        }
    }

    public List<Application> findByJobPostingId(Long jobPostingId) {
        requireNonNull(jobPostingId, "jobPostingId");
        log.info("findByJobPostingId(): jobPostingId={}", jobPostingId);

        try {
            return dbService.findByJobPostingId(jobPostingId);
        } catch (Exception e) {
            log.error("Failed to retrieve applications by jobPostingId: {}", jobPostingId, e);
            throw new ServiceException("Unable to retrieve applications", e);
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
            // If client requested only invalid fields, fall back to dateApplied DESC
            safeSort = Sort.by(Sort.Order.desc("dateApplied"));
        }
        return PageRequest.of(pageable.getPageNumber(), size, safeSort);
    }
}
