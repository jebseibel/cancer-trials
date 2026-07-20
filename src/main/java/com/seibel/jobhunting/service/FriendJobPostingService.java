package com.seibel.jobhunting.service;

import com.seibel.jobhunting.common.domain.FriendJobPosting;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.common.exceptions.ResourceNotFoundException;
import com.seibel.jobhunting.common.exceptions.ServiceException;
import com.seibel.jobhunting.database.db.service.FriendJobPostingDbService;
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
public class FriendJobPostingService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "updatedAt");

    private final FriendJobPostingDbService dbService;

    public FriendJobPostingService(FriendJobPostingDbService dbService) {
        super(FriendJobPosting.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public FriendJobPosting create(FriendJobPosting item) {
        requireNonNull(item, "FriendJobPosting");
        log.info("create(): {}", item);

        try {
            return dbService.create(item.getFriendId(), item.getJobPostingId());
        } catch (Exception e) {
            log.error("Failed to create friend job posting: {}", item, e);
            throw new ServiceException("Unable to create friend job posting", e);
        }
    }

    @Transactional
    public FriendJobPosting update(String extid, FriendJobPosting item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "FriendJobPosting");
        log.info("update(): extid={}, {}", extid, item);

        try {
            FriendJobPosting updated = dbService.update(extid, item.getFriendId(), item.getJobPostingId());
            if (updated == null) {
                throw new ResourceNotFoundException("FriendJobPosting", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update friend job posting: {}", extid, e);
            throw new ServiceException("Unable to update friend job posting", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete friend job posting: {}", extid, e);
            throw new ServiceException("Unable to delete friend job posting", e);
        }
    }

    public FriendJobPosting findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            FriendJobPosting friendJobPosting = dbService.findByExtid(extid);
            if (friendJobPosting == null) {
                throw new ResourceNotFoundException("FriendJobPosting", extid);
            }
            return friendJobPosting;
        } catch (Exception e) {
            log.error("Failed to retrieve friend job posting: {}", extid, e);
            throw new ServiceException("Unable to retrieve friend job posting", e);
        }
    }

    public List<FriendJobPosting> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all friend job postings", e);
            throw new ServiceException("Unable to retrieve friend job postings", e);
        }
    }

    public Page<FriendJobPosting> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve friend job postings (paged)", e);
            throw new ServiceException("Unable to retrieve friend job postings", e);
        }
    }

    public List<FriendJobPosting> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve friend job postings by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve friend job postings", e);
        }
    }

    public List<FriendJobPosting> findByFriendId(Long friendId) {
        requireNonNull(friendId, "friendId");
        log.info("findByFriendId(): friendId={}", friendId);

        try {
            return dbService.findByFriendId(friendId);
        } catch (Exception e) {
            log.error("Failed to retrieve friend job postings by friendId: {}", friendId, e);
            throw new ServiceException("Unable to retrieve friend job postings", e);
        }
    }

    public List<FriendJobPosting> findByJobPostingId(Long jobPostingId) {
        requireNonNull(jobPostingId, "jobPostingId");
        log.info("findByJobPostingId(): jobPostingId={}", jobPostingId);

        try {
            return dbService.findByJobPostingId(jobPostingId);
        } catch (Exception e) {
            log.error("Failed to retrieve friend job postings by jobPostingId: {}", jobPostingId, e);
            throw new ServiceException("Unable to retrieve friend job postings", e);
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
            // If client requested only invalid fields, fall back to createdAt DESC
            safeSort = Sort.by(Sort.Order.desc("createdAt"));
        }
        return PageRequest.of(pageable.getPageNumber(), size, safeSort);
    }
}
