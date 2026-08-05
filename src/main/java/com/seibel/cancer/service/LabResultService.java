package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.LabResult;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.service.LabResultDbService;
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
public class LabResultService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "testName", "loincCode", "status", "effectiveAt", "issuedAt", "createdAt", "updatedAt");

    private final LabResultDbService dbService;

    public LabResultService(LabResultDbService dbService) {
        super(LabResult.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public LabResult create(LabResult item) {
        requireNonNull(item, "LabResult");
        log.info("create(): {}", item.getFhirResourceId());

        try {
            return dbService.create(item);
        } catch (Exception e) {
            log.error("Failed to create labResult: {}", item.getFhirResourceId(), e);
            throw new ServiceException("Unable to create labResult", e);
        }
    }

    @Transactional
    public LabResult update(String extid, LabResult item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "LabResult");
        log.info("update(): extid={}", extid);

        try {
            LabResult updated = dbService.update(extid, item);
            if (updated == null) {
                throw new ResourceNotFoundException("LabResult", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update labResult: {}", extid, e);
            throw new ServiceException("Unable to update labResult", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete labResult: {}", extid, e);
            throw new ServiceException("Unable to delete labResult", e);
        }
    }

    public LabResult findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            LabResult labResult = dbService.findByExtid(extid);
            if (labResult == null) {
                throw new ResourceNotFoundException("LabResult", extid);
            }
            return labResult;
        } catch (Exception e) {
            log.error("Failed to retrieve labResult: {}", extid, e);
            throw new ServiceException("Unable to retrieve labResult", e);
        }
    }

    /** Lookup by Epic's own resource id - used by ingestion to dedup before insert. */
    public LabResult findByFhirResourceId(String fhirResourceId) {
        requireNonBlank(fhirResourceId, "fhirResourceId");
        log.info("findByFhirResourceId(): fhirResourceId={}", fhirResourceId);

        try {
            return dbService.findByFhirResourceId(fhirResourceId);
        } catch (Exception e) {
            log.error("Failed to retrieve labResult by fhirResourceId: {}", fhirResourceId, e);
            throw new ServiceException("Unable to retrieve labResult", e);
        }
    }

    /** All results for one test, newest first - the "this lab over time" view. */
    public List<LabResult> findByLoincCode(String loincCode) {
        requireNonBlank(loincCode, "loincCode");
        log.info("findByLoincCode(): loincCode={}", loincCode);

        try {
            return dbService.findByLoincCode(loincCode);
        } catch (Exception e) {
            log.error("Failed to retrieve labResults by loincCode: {}", loincCode, e);
            throw new ServiceException("Unable to retrieve labResults", e);
        }
    }

    public List<LabResult> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all labResults", e);
            throw new ServiceException("Unable to retrieve labResults", e);
        }
    }

    public Page<LabResult> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve labResults (paged)", e);
            throw new ServiceException("Unable to retrieve labResults", e);
        }
    }

    public List<LabResult> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve labResults by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve labResults", e);
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
            // If client requested only invalid fields, fall back to newest result first
            safeSort = Sort.by(Sort.Order.desc("effectiveAt"));
        }
        return PageRequest.of(pageable.getPageNumber(), size, safeSort);
    }
}
