package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.service.TrialDbService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@Transactional(readOnly = true)
public class TrialService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("briefTitle", "overallStatus", "createdAt", "updatedAt");

    private final TrialDbService dbService;

    public TrialService(TrialDbService dbService) {
        super(Trial.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public Trial create(Trial item) {
        requireNonNull(item, "Trial");
        log.info("create(): {}", item);

        try {
            return dbService.create(item);
        } catch (Exception e) {
            log.error("Failed to create trial: {}", item.getNctId(), e);
            throw new ServiceException("Unable to create trial", e);
        }
    }

    @Transactional
    public Trial update(String extid, Trial item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "Trial");
        log.info("update(): extid={}, {}", extid, item);

        try {
            Trial updated = dbService.update(extid, item);
            if (updated == null) {
                throw new ResourceNotFoundException("Trial", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update trial: {}", extid, e);
            throw new ServiceException("Unable to update trial", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete trial: {}", extid, e);
            throw new ServiceException("Unable to delete trial", e);
        }
    }

    public Trial findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            Trial trial = dbService.findByExtid(extid);
            if (trial == null) {
                throw new ResourceNotFoundException("Trial", extid);
            }
            return trial;
        } catch (Exception e) {
            log.error("Failed to retrieve trial: {}", extid, e);
            throw new ServiceException("Unable to retrieve trial", e);
        }
    }

    public List<Trial> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all trials", e);
            throw new ServiceException("Unable to retrieve trials", e);
        }
    }

    public Optional<Trial> findByNctId(String nctId) {
        requireNonBlank(nctId, "nctId");
        log.info("findByNctId(): nctId={}", nctId);

        try {
            return Optional.ofNullable(dbService.findByNctId(nctId));
        } catch (Exception e) {
            log.error("Failed to retrieve trial by nctId: {}", nctId, e);
            throw new ServiceException("Unable to retrieve trial", e);
        }
    }

    @Transactional
    public Trial upsertByNctId(Trial incoming) {
        requireNonNull(incoming, "Trial");
        requireNonBlank(incoming.getNctId(), "nctId");
        log.info("upsertByNctId(): nctId={}", incoming.getNctId());

        return findByNctId(incoming.getNctId())
                .map(existing -> update(existing.getExtid(), incoming))
                .orElseGet(() -> create(incoming));
    }

    public Page<Trial> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve trials (paged)", e);
            throw new ServiceException("Unable to retrieve trials", e);
        }
    }

    public List<Trial> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve trials by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve trials", e);
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
            safeSort = Sort.by(Sort.Order.asc("briefTitle"));
        }
        return PageRequest.of(pageable.getPageNumber(), size, safeSort);
    }
}
