package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.EligibilityRule;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.service.EligibilityRuleDbService;
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
public class EligibilityRuleService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("sortOrder", "createdAt", "updatedAt");

    private final EligibilityRuleDbService dbService;

    public EligibilityRuleService(EligibilityRuleDbService dbService) {
        super(EligibilityRule.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public EligibilityRule create(EligibilityRule item) {
        requireNonNull(item, "EligibilityRule");
        log.info("create(): {}", item);

        try {
            return dbService.create(item);
        } catch (Exception e) {
            log.error("Failed to create eligibilityRule: {}", item, e);
            throw new ServiceException("Unable to create eligibilityRule", e);
        }
    }

    @Transactional
    public EligibilityRule update(String extid, EligibilityRule item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "EligibilityRule");
        log.info("update(): extid={}, {}", extid, item);

        try {
            EligibilityRule updated = dbService.update(extid, item);
            if (updated == null) {
                throw new ResourceNotFoundException("EligibilityRule", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update eligibilityRule: {}", extid, e);
            throw new ServiceException("Unable to update eligibilityRule", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete eligibilityRule: {}", extid, e);
            throw new ServiceException("Unable to delete eligibilityRule", e);
        }
    }

    public EligibilityRule findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            EligibilityRule item = dbService.findByExtid(extid);
            if (item == null) {
                throw new ResourceNotFoundException("EligibilityRule", extid);
            }
            return item;
        } catch (Exception e) {
            log.error("Failed to retrieve eligibilityRule: {}", extid, e);
            throw new ServiceException("Unable to retrieve eligibilityRule", e);
        }
    }

    public List<EligibilityRule> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all eligibilityRules", e);
            throw new ServiceException("Unable to retrieve eligibilityRules", e);
        }
    }

    public List<EligibilityRule> findByTrialId(Long trialId) {
        requireNonNull(trialId, "trialId");
        log.info("findByTrialId(): trialId={}", trialId);

        try {
            return dbService.findByTrialId(trialId);
        } catch (Exception e) {
            log.error("Failed to retrieve eligibilityRules by trialId: {}", trialId, e);
            throw new ServiceException("Unable to retrieve eligibilityRules", e);
        }
    }

    public Page<EligibilityRule> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve eligibilityRules (paged)", e);
            throw new ServiceException("Unable to retrieve eligibilityRules", e);
        }
    }

    public List<EligibilityRule> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve eligibilityRules by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve eligibilityRules", e);
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
            safeSort = Sort.by(Sort.Order.asc("sortOrder"));
        }
        return PageRequest.of(pageable.getPageNumber(), size, safeSort);
    }
}
