package com.seibel.jobs.service;

import com.seibel.jobs.common.domain.Friend;
import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.common.exceptions.ResourceNotFoundException;
import com.seibel.jobs.common.exceptions.ServiceException;
import com.seibel.jobs.database.db.service.FriendDbService;
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
public class FriendService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "lastContactedAt", "createdAt", "updatedAt");

    private final FriendDbService dbService;

    public FriendService(FriendDbService dbService) {
        super(Friend.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public Friend create(Friend item) {
        requireNonNull(item, "Friend");
        log.info("create(): {}", item);

        try {
            return dbService.create(item.getName(), item.getRelationship(), item.getEmail(), item.getPhone(),
                    item.getLinkedinUrl(), item.getLastContactedAt(), item.getNotes());
        } catch (Exception e) {
            log.error("Failed to create friend: {}", item.getName(), e);
            throw new ServiceException("Unable to create friend", e);
        }
    }

    @Transactional
    public Friend update(String extid, Friend item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "Friend");
        log.info("update(): extid={}, {}", extid, item);

        try {
            Friend updated = dbService.update(extid, item.getName(), item.getRelationship(), item.getEmail(), item.getPhone(),
                    item.getLinkedinUrl(), item.getLastContactedAt(), item.getNotes());
            if (updated == null) {
                throw new ResourceNotFoundException("Friend", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update friend: {}", extid, e);
            throw new ServiceException("Unable to update friend", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete friend: {}", extid, e);
            throw new ServiceException("Unable to delete friend", e);
        }
    }

    public Friend findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            Friend friend = dbService.findByExtid(extid);
            if (friend == null) {
                throw new ResourceNotFoundException("Friend", extid);
            }
            return friend;
        } catch (Exception e) {
            log.error("Failed to retrieve friend: {}", extid, e);
            throw new ServiceException("Unable to retrieve friend", e);
        }
    }

    public List<Friend> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all friends", e);
            throw new ServiceException("Unable to retrieve friends", e);
        }
    }

    public Page<Friend> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve friends (paged)", e);
            throw new ServiceException("Unable to retrieve friends", e);
        }
    }

    public List<Friend> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve friends by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve friends", e);
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
