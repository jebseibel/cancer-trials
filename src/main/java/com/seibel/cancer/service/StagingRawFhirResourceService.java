package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.StagingRawFhirResource;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.service.StagingRawFhirResourceDbService;
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
public class StagingRawFhirResourceService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("fhirResourceId", "resourceType", "fetchedAt", "normalizedAt", "createdAt", "updatedAt");

    private final StagingRawFhirResourceDbService dbService;

    public StagingRawFhirResourceService(StagingRawFhirResourceDbService dbService) {
        super(StagingRawFhirResource.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public StagingRawFhirResource create(StagingRawFhirResource item) {
        requireNonNull(item, "StagingRawFhirResource");
        log.info("create(): {}", item);

        try {
            return dbService.create(item.getResourceType(), item.getFhirResourceId(), item.getRawPayload(),
                    item.getFetchedAt(), item.getNormalizedAt(), item.getNormalizationError());
        } catch (Exception e) {
            log.error("Failed to create stagingRawFhirResource: {}", item.getFhirResourceId(), e);
            throw new ServiceException("Unable to create stagingRawFhirResource", e);
        }
    }

    @Transactional
    public StagingRawFhirResource update(String extid, StagingRawFhirResource item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "StagingRawFhirResource");
        log.info("update(): extid={}, {}", extid, item);

        try {
            StagingRawFhirResource updated = dbService.update(extid, item.getResourceType(), item.getFhirResourceId(), item.getRawPayload(),
                    item.getFetchedAt(), item.getNormalizedAt(), item.getNormalizationError());
            if (updated == null) {
                throw new ResourceNotFoundException("StagingRawFhirResource", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update stagingRawFhirResource: {}", extid, e);
            throw new ServiceException("Unable to update stagingRawFhirResource", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete stagingRawFhirResource: {}", extid, e);
            throw new ServiceException("Unable to delete stagingRawFhirResource", e);
        }
    }

    public StagingRawFhirResource findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            StagingRawFhirResource stagingRawFhirResource = dbService.findByExtid(extid);
            if (stagingRawFhirResource == null) {
                throw new ResourceNotFoundException("StagingRawFhirResource", extid);
            }
            return stagingRawFhirResource;
        } catch (Exception e) {
            log.error("Failed to retrieve stagingRawFhirResource: {}", extid, e);
            throw new ServiceException("Unable to retrieve stagingRawFhirResource", e);
        }
    }

    public List<StagingRawFhirResource> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all stagingRawFhirResources", e);
            throw new ServiceException("Unable to retrieve stagingRawFhirResources", e);
        }
    }

    public List<StagingRawFhirResource> findPending(int maxRows) {
        log.info("findPending(): maxRows={}", maxRows);

        try {
            return dbService.findPending(maxRows);
        } catch (Exception e) {
            log.error("Failed to retrieve pending stagingRawFhirResources", e);
            throw new ServiceException("Unable to retrieve pending stagingRawFhirResources", e);
        }
    }

    public Page<StagingRawFhirResource> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve stagingRawFhirResources (paged)", e);
            throw new ServiceException("Unable to retrieve stagingRawFhirResources", e);
        }
    }

    public List<StagingRawFhirResource> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve stagingRawFhirResources by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve stagingRawFhirResources", e);
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
