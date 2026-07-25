package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.Outcome;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.service.OutcomeDbService;
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
public class OutcomeService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("outcomeType", "measure", "createdAt", "updatedAt");

    private final OutcomeDbService dbService;

    public OutcomeService(OutcomeDbService dbService) {
        super(Outcome.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public Outcome create(Outcome item) {
        requireNonNull(item, "Outcome");
        log.info("create(): {}", item);

        try {
            return dbService.create(item.getTrialId(), item.getOutcomeType(), item.getMeasure(),
                    item.getDescription(), item.getTimeFrame());
        } catch (Exception e) {
            log.error("Failed to create outcome: {}", item.getMeasure(), e);
            throw new ServiceException("Unable to create outcome", e);
        }
    }

    @Transactional
    public Outcome update(String extid, Outcome item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "Outcome");
        log.info("update(): extid={}, {}", extid, item);

        try {
            Outcome updated = dbService.update(extid, item.getTrialId(), item.getOutcomeType(), item.getMeasure(),
                    item.getDescription(), item.getTimeFrame());
            if (updated == null) {
                throw new ResourceNotFoundException("Outcome", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update outcome: {}", extid, e);
            throw new ServiceException("Unable to update outcome", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete outcome: {}", extid, e);
            throw new ServiceException("Unable to delete outcome", e);
        }
    }

    public Outcome findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            Outcome outcome = dbService.findByExtid(extid);
            if (outcome == null) {
                throw new ResourceNotFoundException("Outcome", extid);
            }
            return outcome;
        } catch (Exception e) {
            log.error("Failed to retrieve outcome: {}", extid, e);
            throw new ServiceException("Unable to retrieve outcome", e);
        }
    }

    public List<Outcome> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all outcomes", e);
            throw new ServiceException("Unable to retrieve outcomes", e);
        }
    }

    public Page<Outcome> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve outcomes (paged)", e);
            throw new ServiceException("Unable to retrieve outcomes", e);
        }
    }

    public List<Outcome> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve outcomes by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve outcomes", e);
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
            // If client requested only invalid fields, fall back to measure ASC
            safeSort = Sort.by(Sort.Order.asc("measure"));
        }
        return PageRequest.of(pageable.getPageNumber(), size, safeSort);
    }
}
