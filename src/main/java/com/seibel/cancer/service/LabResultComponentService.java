package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.LabResultComponent;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.service.LabResultComponentDbService;
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
public class LabResultComponentService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "componentName", "loincCode", "createdAt", "updatedAt");

    private final LabResultComponentDbService dbService;

    public LabResultComponentService(LabResultComponentDbService dbService) {
        super(LabResultComponent.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public LabResultComponent create(LabResultComponent item) {
        requireNonNull(item, "LabResultComponent");
        log.info("create(): {}", item.getComponentName());

        try {
            return dbService.create(item);
        } catch (Exception e) {
            log.error("Failed to create labResultComponent: {}", item.getComponentName(), e);
            throw new ServiceException("Unable to create labResultComponent", e);
        }
    }

    @Transactional
    public LabResultComponent update(String extid, LabResultComponent item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "LabResultComponent");
        log.info("update(): extid={}", extid);

        try {
            LabResultComponent updated = dbService.update(extid, item);
            if (updated == null) {
                throw new ResourceNotFoundException("LabResultComponent", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update labResultComponent: {}", extid, e);
            throw new ServiceException("Unable to update labResultComponent", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete labResultComponent: {}", extid, e);
            throw new ServiceException("Unable to delete labResultComponent", e);
        }
    }

    public LabResultComponent findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            LabResultComponent labResultComponent = dbService.findByExtid(extid);
            if (labResultComponent == null) {
                throw new ResourceNotFoundException("LabResultComponent", extid);
            }
            return labResultComponent;
        } catch (Exception e) {
            log.error("Failed to retrieve labResultComponent: {}", extid, e);
            throw new ServiceException("Unable to retrieve labResultComponent", e);
        }
    }

    /** All components of one panel. */
    public List<LabResultComponent> findByLabResultId(Long labResultId) {
        requireNonNull(labResultId, "labResultId");
        log.info("findByLabResultId(): labResultId={}", labResultId);

        try {
            return dbService.findByLabResultId(labResultId);
        } catch (Exception e) {
            log.error("Failed to retrieve labResultComponents by labResultId: {}", labResultId, e);
            throw new ServiceException("Unable to retrieve labResultComponents", e);
        }
    }

    public List<LabResultComponent> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all labResultComponents", e);
            throw new ServiceException("Unable to retrieve labResultComponents", e);
        }
    }

    public Page<LabResultComponent> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve labResultComponents (paged)", e);
            throw new ServiceException("Unable to retrieve labResultComponents", e);
        }
    }

    public List<LabResultComponent> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve labResultComponents by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve labResultComponents", e);
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
            // If client requested only invalid fields, fall back to componentName ASC
            safeSort = Sort.by(Sort.Order.asc("componentName"));
        }
        return PageRequest.of(pageable.getPageNumber(), size, safeSort);
    }
}
