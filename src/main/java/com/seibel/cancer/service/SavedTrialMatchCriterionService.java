package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.SavedTrialMatchCriterion;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.service.SavedTrialMatchCriterionDbService;
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
public class SavedTrialMatchCriterionService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "score", "ordinal", "createdAt", "updatedAt");

    private final SavedTrialMatchCriterionDbService dbService;

    public SavedTrialMatchCriterionService(SavedTrialMatchCriterionDbService dbService) {
        super(SavedTrialMatchCriterion.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public SavedTrialMatchCriterion create(SavedTrialMatchCriterion item) {
        requireNonNull(item, "SavedTrialMatchCriterion");
        log.info("create(): trialMatchId={}", item.getTrialMatchId());

        try {
            return dbService.create(item);
        } catch (Exception e) {
            log.error("Failed to create trialMatchCriterion for trialMatchId: {}", item.getTrialMatchId(), e);
            throw new ServiceException("Unable to create trialMatchCriterion", e);
        }
    }

    @Transactional
    public SavedTrialMatchCriterion update(String extid, SavedTrialMatchCriterion item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "SavedTrialMatchCriterion");
        log.info("update(): extid={}", extid);

        try {
            SavedTrialMatchCriterion updated = dbService.update(extid, item);
            if (updated == null) {
                throw new ResourceNotFoundException("SavedTrialMatchCriterion", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update trialMatchCriterion: {}", extid, e);
            throw new ServiceException("Unable to update trialMatchCriterion", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete trialMatchCriterion: {}", extid, e);
            throw new ServiceException("Unable to delete trialMatchCriterion", e);
        }
    }

    public SavedTrialMatchCriterion findByExtid(String extid) {
        requireNonBlank(extid, "extid");

        try {
            SavedTrialMatchCriterion found = dbService.findByExtid(extid);
            if (found == null) {
                throw new ResourceNotFoundException("SavedTrialMatchCriterion", extid);
            }
            return found;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to find trialMatchCriterion: {}", extid, e);
            throw new ServiceException("Unable to find trialMatchCriterion", e);
        }
    }

    public List<SavedTrialMatchCriterion> findAll() {
        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to find all trialMatchCriteria", e);
            throw new ServiceException("Unable to find trialMatchCriteria", e);
        }
    }

    public Page<SavedTrialMatchCriterion> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);

        try {
            return activeEnum == null ? dbService.findAll(safe) : dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to find trialMatchCriteria page", e);
            throw new ServiceException("Unable to find trialMatchCriteria", e);
        }
    }

    public List<SavedTrialMatchCriterion> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to find trialMatchCriteria by active: {}", activeEnum, e);
            throw new ServiceException("Unable to find trialMatchCriteria", e);
        }
    }

    /** The evidence behind one match, best-scoring first. */
    public List<SavedTrialMatchCriterion> findByTrialMatchId(Long trialMatchId) {
        requireNonNull(trialMatchId, "trialMatchId");

        try {
            return dbService.findByTrialMatchId(trialMatchId);
        } catch (Exception e) {
            log.error("Failed to find trialMatchCriteria by trialMatchId: {}", trialMatchId, e);
            throw new ServiceException("Unable to find trialMatchCriteria", e);
        }
    }

    private Pageable enforceCapsAndWhitelist(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "score"));
        }

        int size = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);

        Sort safeSort = Sort.by(pageable.getSort().stream()
                .filter(order -> ALLOWED_SORT_FIELDS.contains(order.getProperty()))
                .toList());

        if (safeSort.isEmpty()) {
            safeSort = Sort.by(Sort.Direction.DESC, "score");
        }

        return PageRequest.of(pageable.getPageNumber(), size, safeSort);
    }
}
