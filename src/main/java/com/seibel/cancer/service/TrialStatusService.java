package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.TrialStatus;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.service.TrialStatusDbService;
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
public class TrialStatusService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("status", "statusChangedAt", "createdAt", "updatedAt");

    private final TrialStatusDbService dbService;

    public TrialStatusService(TrialStatusDbService dbService) {
        super(TrialStatus.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public TrialStatus create(TrialStatus item) {
        requireNonNull(item, "TrialStatus");
        log.info("create(): {}", item);

        try {
            return dbService.create(item.getTrialId(), item.getPatientId(), item.getStatus(),
                    item.getNotes(), item.getStatusChangedAt());
        } catch (Exception e) {
            log.error("Failed to create trial status: {}", item, e);
            throw new ServiceException("Unable to create trial status", e);
        }
    }

    @Transactional
    public TrialStatus update(String extid, TrialStatus item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "TrialStatus");
        log.info("update(): extid={}, {}", extid, item);

        try {
            TrialStatus updated = dbService.update(extid, item.getTrialId(), item.getPatientId(),
                    item.getStatus(), item.getNotes(), item.getStatusChangedAt());
            if (updated == null) {
                throw new ResourceNotFoundException("TrialStatus", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update trial status: {}", extid, e);
            throw new ServiceException("Unable to update trial status", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete trial status: {}", extid, e);
            throw new ServiceException("Unable to delete trial status", e);
        }
    }

    public TrialStatus findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            TrialStatus trialStatus = dbService.findByExtid(extid);
            if (trialStatus == null) {
                throw new ResourceNotFoundException("TrialStatus", extid);
            }
            return trialStatus;
        } catch (Exception e) {
            log.error("Failed to retrieve trial status: {}", extid, e);
            throw new ServiceException("Unable to retrieve trial status", e);
        }
    }

    public List<TrialStatus> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all trial statuses", e);
            throw new ServiceException("Unable to retrieve trial statuses", e);
        }
    }

    public List<TrialStatus> findByPatientId(Long patientId) {
        requireNonNull(patientId, "patientId");
        log.info("findByPatientId(): patientId={}", patientId);

        try {
            return dbService.findByPatientId(patientId);
        } catch (Exception e) {
            log.error("Failed to retrieve trial statuses by patientId: {}", patientId, e);
            throw new ServiceException("Unable to retrieve trial statuses", e);
        }
    }

    public Page<TrialStatus> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve trial statuses (paged)", e);
            throw new ServiceException("Unable to retrieve trial statuses", e);
        }
    }

    public List<TrialStatus> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve trial statuses by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve trial statuses", e);
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
            // If client requested only invalid fields, fall back to statusChangedAt ASC
            safeSort = Sort.by(Sort.Order.asc("statusChangedAt"));
        }
        return PageRequest.of(pageable.getPageNumber(), size, safeSort);
    }
}
