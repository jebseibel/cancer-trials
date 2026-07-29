package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.Intervention;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.service.InterventionDbService;
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
public class InterventionService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "type", "createdAt", "updatedAt");

    private final InterventionDbService dbService;

    public InterventionService(InterventionDbService dbService) {
        super(Intervention.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public Intervention create(Intervention item) {
        requireNonNull(item, "Intervention");
        log.info("create(): {}", item);

        try {
            return dbService.create(item.getTrialId(), item.getType(), item.getName(), item.getDescription());
        } catch (Exception e) {
            log.error("Failed to create intervention: {}", item.getName(), e);
            throw new ServiceException("Unable to create intervention", e);
        }
    }

    @Transactional
    public Intervention update(String extid, Intervention item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "Intervention");
        log.info("update(): extid={}, {}", extid, item);

        try {
            Intervention updated = dbService.update(extid, item.getTrialId(), item.getType(), item.getName(), item.getDescription());
            if (updated == null) {
                throw new ResourceNotFoundException("Intervention", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update intervention: {}", extid, e);
            throw new ServiceException("Unable to update intervention", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete intervention: {}", extid, e);
            throw new ServiceException("Unable to delete intervention", e);
        }
    }

    public Intervention findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            Intervention intervention = dbService.findByExtid(extid);
            if (intervention == null) {
                throw new ResourceNotFoundException("Intervention", extid);
            }
            return intervention;
        } catch (Exception e) {
            log.error("Failed to retrieve intervention: {}", extid, e);
            throw new ServiceException("Unable to retrieve intervention", e);
        }
    }

    public List<Intervention> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all interventions", e);
            throw new ServiceException("Unable to retrieve interventions", e);
        }
    }

    public List<Intervention> findByTrialId(Long trialId) {
        requireNonNull(trialId, "trialId");
        log.info("findByTrialId(): trialId={}", trialId);

        try {
            return dbService.findByTrialId(trialId);
        } catch (Exception e) {
            log.error("Failed to retrieve interventions by trialId: {}", trialId, e);
            throw new ServiceException("Unable to retrieve interventions", e);
        }
    }

    public Page<Intervention> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve interventions (paged)", e);
            throw new ServiceException("Unable to retrieve interventions", e);
        }
    }

    public List<Intervention> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve interventions by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve interventions", e);
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
            // If client requested only invalid fields, fall back to name ASC
            safeSort = Sort.by(Sort.Order.asc("name"));
        }
        return PageRequest.of(pageable.getPageNumber(), size, safeSort);
    }
}
