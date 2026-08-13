package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.SavedTrialMatch;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.service.SavedTrialMatchDbService;
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
public class SavedTrialMatchService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "topScore", "matchRank", "matchedAt", "createdAt", "updatedAt");

    private final SavedTrialMatchDbService dbService;

    public SavedTrialMatchService(SavedTrialMatchDbService dbService) {
        super(SavedTrialMatch.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public SavedTrialMatch create(SavedTrialMatch item) {
        requireNonNull(item, "SavedTrialMatch");
        log.info("create(): searchRunId={}, trialId={}", item.getSearchRunId(), item.getTrialId());

        try {
            return dbService.create(item);
        } catch (Exception e) {
            log.error("Failed to create trialMatch for searchRunId: {}", item.getSearchRunId(), e);
            throw new ServiceException("Unable to create trialMatch", e);
        }
    }

    @Transactional
    public SavedTrialMatch update(String extid, SavedTrialMatch item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "SavedTrialMatch");
        log.info("update(): extid={}", extid);

        try {
            SavedTrialMatch updated = dbService.update(extid, item);
            if (updated == null) {
                throw new ResourceNotFoundException("SavedTrialMatch", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update trialMatch: {}", extid, e);
            throw new ServiceException("Unable to update trialMatch", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete trialMatch: {}", extid, e);
            throw new ServiceException("Unable to delete trialMatch", e);
        }
    }

    public SavedTrialMatch findByExtid(String extid) {
        requireNonBlank(extid, "extid");

        try {
            SavedTrialMatch found = dbService.findByExtid(extid);
            if (found == null) {
                throw new ResourceNotFoundException("SavedTrialMatch", extid);
            }
            return found;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to find trialMatch: {}", extid, e);
            throw new ServiceException("Unable to find trialMatch", e);
        }
    }

    public List<SavedTrialMatch> findAll() {
        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to find all trialMatches", e);
            throw new ServiceException("Unable to find trialMatches", e);
        }
    }

    public Page<SavedTrialMatch> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);

        try {
            return activeEnum == null ? dbService.findAll(safe) : dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to find trialMatches page", e);
            throw new ServiceException("Unable to find trialMatches", e);
        }
    }

    public List<SavedTrialMatch> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to find trialMatches by active: {}", activeEnum, e);
            throw new ServiceException("Unable to find trialMatches", e);
        }
    }

    /** Every match from one search run - the unit a user actually looks at. */
    public List<SavedTrialMatch> findBySearchRunId(String searchRunId) {
        requireNonBlank(searchRunId, "searchRunId");

        try {
            return dbService.findBySearchRunId(searchRunId);
        } catch (Exception e) {
            log.error("Failed to find trialMatches by searchRunId: {}", searchRunId, e);
            throw new ServiceException("Unable to find trialMatches", e);
        }
    }

    public List<SavedTrialMatch> findByPatientId(Long patientId) {
        requireNonNull(patientId, "patientId");

        try {
            return dbService.findByPatientId(patientId);
        } catch (Exception e) {
            log.error("Failed to find trialMatches by patientId: {}", patientId, e);
            throw new ServiceException("Unable to find trialMatches", e);
        }
    }

    public List<SavedTrialMatch> findByTrialId(Long trialId) {
        requireNonNull(trialId, "trialId");

        try {
            return dbService.findByTrialId(trialId);
        } catch (Exception e) {
            log.error("Failed to find trialMatches by trialId: {}", trialId, e);
            throw new ServiceException("Unable to find trialMatches", e);
        }
    }

    private Pageable enforceCapsAndWhitelist(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "topScore"));
        }

        int size = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);

        Sort safeSort = Sort.by(pageable.getSort().stream()
                .filter(order -> ALLOWED_SORT_FIELDS.contains(order.getProperty()))
                .toList());

        if (safeSort.isEmpty()) {
            safeSort = Sort.by(Sort.Direction.DESC, "topScore");
        }

        return PageRequest.of(pageable.getPageNumber(), size, safeSort);
    }
}
