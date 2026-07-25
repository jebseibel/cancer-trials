package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.TrialSource;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.service.TrialSourceDbService;
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
public class TrialSourceService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("code", "name", "createdAt", "updatedAt");

    private final TrialSourceDbService dbService;

    public TrialSourceService(TrialSourceDbService dbService) {
        super(TrialSource.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public TrialSource create(TrialSource item) {
        requireNonNull(item, "TrialSource");
        log.info("create(): {}", item);

        try {
            return dbService.create(item.getCode(), item.getName(), item.getBaseUrl());
        } catch (Exception e) {
            log.error("Failed to create trialSource: {}", item.getCode(), e);
            throw new ServiceException("Unable to create trialSource", e);
        }
    }

    @Transactional
    public TrialSource update(String extid, TrialSource item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "TrialSource");
        log.info("update(): extid={}, {}", extid, item);

        try {
            TrialSource updated = dbService.update(extid, item.getCode(), item.getName(), item.getBaseUrl());
            if (updated == null) {
                throw new ResourceNotFoundException("TrialSource", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update trialSource: {}", extid, e);
            throw new ServiceException("Unable to update trialSource", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete trialSource: {}", extid, e);
            throw new ServiceException("Unable to delete trialSource", e);
        }
    }

    public TrialSource findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            TrialSource trialSource = dbService.findByExtid(extid);
            if (trialSource == null) {
                throw new ResourceNotFoundException("TrialSource", extid);
            }
            return trialSource;
        } catch (Exception e) {
            log.error("Failed to retrieve trialSource: {}", extid, e);
            throw new ServiceException("Unable to retrieve trialSource", e);
        }
    }

    public List<TrialSource> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all trialSources", e);
            throw new ServiceException("Unable to retrieve trialSources", e);
        }
    }

    public Page<TrialSource> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve trialSources (paged)", e);
            throw new ServiceException("Unable to retrieve trialSources", e);
        }
    }

    public List<TrialSource> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve trialSources by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve trialSources", e);
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
            // If client requested only invalid fields, fall back to code ASC
            safeSort = Sort.by(Sort.Order.asc("code"));
        }
        return PageRequest.of(pageable.getPageNumber(), size, safeSort);
    }
}
