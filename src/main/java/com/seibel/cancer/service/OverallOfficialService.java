package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.OverallOfficial;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.service.OverallOfficialDbService;
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
public class OverallOfficialService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "createdAt", "updatedAt");

    private final OverallOfficialDbService dbService;

    public OverallOfficialService(OverallOfficialDbService dbService) {
        super(OverallOfficial.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public OverallOfficial create(OverallOfficial item) {
        requireNonNull(item, "OverallOfficial");
        log.info("create(): {}", item);

        try {
            return dbService.create(item.getTrialId(), item.getName(), item.getAffiliation(), item.getRole());
        } catch (Exception e) {
            log.error("Failed to create overallOfficial: {}", item.getName(), e);
            throw new ServiceException("Unable to create overallOfficial", e);
        }
    }

    @Transactional
    public OverallOfficial update(String extid, OverallOfficial item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "OverallOfficial");
        log.info("update(): extid={}, {}", extid, item);

        try {
            OverallOfficial updated = dbService.update(extid, item.getTrialId(), item.getName(), item.getAffiliation(), item.getRole());
            if (updated == null) {
                throw new ResourceNotFoundException("OverallOfficial", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update overallOfficial: {}", extid, e);
            throw new ServiceException("Unable to update overallOfficial", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete overallOfficial: {}", extid, e);
            throw new ServiceException("Unable to delete overallOfficial", e);
        }
    }

    public OverallOfficial findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            OverallOfficial overallOfficial = dbService.findByExtid(extid);
            if (overallOfficial == null) {
                throw new ResourceNotFoundException("OverallOfficial", extid);
            }
            return overallOfficial;
        } catch (Exception e) {
            log.error("Failed to retrieve overallOfficial: {}", extid, e);
            throw new ServiceException("Unable to retrieve overallOfficial", e);
        }
    }

    public List<OverallOfficial> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all overallOfficials", e);
            throw new ServiceException("Unable to retrieve overallOfficials", e);
        }
    }

    public List<OverallOfficial> findByTrialId(Long trialId) {
        requireNonNull(trialId, "trialId");
        log.info("findByTrialId(): trialId={}", trialId);

        try {
            return dbService.findByTrialId(trialId);
        } catch (Exception e) {
            log.error("Failed to retrieve overallOfficials by trialId: {}", trialId, e);
            throw new ServiceException("Unable to retrieve overallOfficials", e);
        }
    }

    public Page<OverallOfficial> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve overallOfficials (paged)", e);
            throw new ServiceException("Unable to retrieve overallOfficials", e);
        }
    }

    public List<OverallOfficial> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve overallOfficials by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve overallOfficials", e);
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
