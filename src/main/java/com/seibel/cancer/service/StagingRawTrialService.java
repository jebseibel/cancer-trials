package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.StagingRawTrial;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.service.StagingRawTrialDbService;
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
public class StagingRawTrialService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("sourceTrialId", "fetchedAt", "normalizedAt", "createdAt", "updatedAt");

    private final StagingRawTrialDbService dbService;

    public StagingRawTrialService(StagingRawTrialDbService dbService) {
        super(StagingRawTrial.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public StagingRawTrial create(StagingRawTrial item) {
        requireNonNull(item, "StagingRawTrial");
        log.info("create(): {}", item);

        try {
            return dbService.create(item.getTrialSourceId(), item.getSourceTrialId(), item.getRawPayload(),
                    item.getFetchedAt(), item.getNormalizedAt(), item.getNormalizationError());
        } catch (Exception e) {
            log.error("Failed to create stagingRawTrial: {}", item.getSourceTrialId(), e);
            throw new ServiceException("Unable to create stagingRawTrial", e);
        }
    }

    @Transactional
    public StagingRawTrial update(String extid, StagingRawTrial item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "StagingRawTrial");
        log.info("update(): extid={}, {}", extid, item);

        try {
            StagingRawTrial updated = dbService.update(extid, item.getTrialSourceId(), item.getSourceTrialId(), item.getRawPayload(),
                    item.getFetchedAt(), item.getNormalizedAt(), item.getNormalizationError());
            if (updated == null) {
                throw new ResourceNotFoundException("StagingRawTrial", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update stagingRawTrial: {}", extid, e);
            throw new ServiceException("Unable to update stagingRawTrial", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete stagingRawTrial: {}", extid, e);
            throw new ServiceException("Unable to delete stagingRawTrial", e);
        }
    }

    public StagingRawTrial findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            StagingRawTrial stagingRawTrial = dbService.findByExtid(extid);
            if (stagingRawTrial == null) {
                throw new ResourceNotFoundException("StagingRawTrial", extid);
            }
            return stagingRawTrial;
        } catch (Exception e) {
            log.error("Failed to retrieve stagingRawTrial: {}", extid, e);
            throw new ServiceException("Unable to retrieve stagingRawTrial", e);
        }
    }

    public List<StagingRawTrial> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all stagingRawTrials", e);
            throw new ServiceException("Unable to retrieve stagingRawTrials", e);
        }
    }

    public List<StagingRawTrial> findPending(int maxRows) {
        log.info("findPending(): maxRows={}", maxRows);

        try {
            return dbService.findPending(maxRows);
        } catch (Exception e) {
            log.error("Failed to retrieve pending stagingRawTrials", e);
            throw new ServiceException("Unable to retrieve pending stagingRawTrials", e);
        }
    }

    public Page<StagingRawTrial> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve stagingRawTrials (paged)", e);
            throw new ServiceException("Unable to retrieve stagingRawTrials", e);
        }
    }

    public List<StagingRawTrial> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve stagingRawTrials by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve stagingRawTrials", e);
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
            // If client requested only invalid fields, fall back to fetchedAt DESC
            safeSort = Sort.by(Sort.Order.desc("fetchedAt"));
        }
        return PageRequest.of(pageable.getPageNumber(), size, safeSort);
    }
}
