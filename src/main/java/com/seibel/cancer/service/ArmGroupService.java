package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.ArmGroup;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.service.ArmGroupDbService;
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
public class ArmGroupService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("label", "type", "createdAt", "updatedAt");

    private final ArmGroupDbService dbService;

    public ArmGroupService(ArmGroupDbService dbService) {
        super(ArmGroup.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public ArmGroup create(ArmGroup item) {
        requireNonNull(item, "ArmGroup");
        log.info("create(): {}", item);

        try {
            return dbService.create(item.getTrialId(), item.getLabel(), item.getType(), item.getDescription());
        } catch (Exception e) {
            log.error("Failed to create armGroup: {}", item.getLabel(), e);
            throw new ServiceException("Unable to create armGroup", e);
        }
    }

    @Transactional
    public ArmGroup update(String extid, ArmGroup item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "ArmGroup");
        log.info("update(): extid={}, {}", extid, item);

        try {
            ArmGroup updated = dbService.update(extid, item.getTrialId(), item.getLabel(), item.getType(), item.getDescription());
            if (updated == null) {
                throw new ResourceNotFoundException("ArmGroup", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update armGroup: {}", extid, e);
            throw new ServiceException("Unable to update armGroup", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete armGroup: {}", extid, e);
            throw new ServiceException("Unable to delete armGroup", e);
        }
    }

    public ArmGroup findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            ArmGroup armGroup = dbService.findByExtid(extid);
            if (armGroup == null) {
                throw new ResourceNotFoundException("ArmGroup", extid);
            }
            return armGroup;
        } catch (Exception e) {
            log.error("Failed to retrieve armGroup: {}", extid, e);
            throw new ServiceException("Unable to retrieve armGroup", e);
        }
    }

    public List<ArmGroup> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all armGroups", e);
            throw new ServiceException("Unable to retrieve armGroups", e);
        }
    }

    public List<ArmGroup> findByTrialId(Long trialId) {
        requireNonNull(trialId, "trialId");
        log.info("findByTrialId(): trialId={}", trialId);

        try {
            return dbService.findByTrialId(trialId);
        } catch (Exception e) {
            log.error("Failed to retrieve armGroups by trialId: {}", trialId, e);
            throw new ServiceException("Unable to retrieve armGroups", e);
        }
    }

    public Page<ArmGroup> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve armGroups (paged)", e);
            throw new ServiceException("Unable to retrieve armGroups", e);
        }
    }

    public List<ArmGroup> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve armGroups by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve armGroups", e);
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
            // If client requested only invalid fields, fall back to label ASC
            safeSort = Sort.by(Sort.Order.asc("label"));
        }
        return PageRequest.of(pageable.getPageNumber(), size, safeSort);
    }
}
