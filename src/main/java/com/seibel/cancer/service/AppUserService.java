package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.AppUser;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.service.AppUserDbService;
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
public class AppUserService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("username", "displayName", "createdAt", "updatedAt");

    private final AppUserDbService dbService;

    public AppUserService(AppUserDbService dbService) {
        super(AppUser.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public AppUser create(AppUser item) {
        requireNonNull(item, "AppUser");
        log.info("create(): {}", item);

        try {
            return dbService.create(item.getUsername(), item.getPasswordHash(), item.getDisplayName());
        } catch (Exception e) {
            log.error("Failed to create app user: {}", item.getUsername(), e);
            throw new ServiceException("Unable to create app user", e);
        }
    }

    @Transactional
    public AppUser update(String extid, AppUser item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "AppUser");
        log.info("update(): extid={}, {}", extid, item);

        try {
            AppUser updated = dbService.update(extid, item.getUsername(), item.getPasswordHash(), item.getDisplayName());
            if (updated == null) {
                throw new ResourceNotFoundException("AppUser", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update app user: {}", extid, e);
            throw new ServiceException("Unable to update app user", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete app user: {}", extid, e);
            throw new ServiceException("Unable to delete app user", e);
        }
    }

    public AppUser findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            AppUser appUser = dbService.findByExtid(extid);
            if (appUser == null) {
                throw new ResourceNotFoundException("AppUser", extid);
            }
            return appUser;
        } catch (Exception e) {
            log.error("Failed to retrieve app user: {}", extid, e);
            throw new ServiceException("Unable to retrieve app user", e);
        }
    }

    public List<AppUser> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all app users", e);
            throw new ServiceException("Unable to retrieve app users", e);
        }
    }

    public Page<AppUser> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve app users (paged)", e);
            throw new ServiceException("Unable to retrieve app users", e);
        }
    }

    public List<AppUser> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve app users by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve app users", e);
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
            safeSort = Sort.by(Sort.Order.asc("username"));
        }
        return PageRequest.of(pageable.getPageNumber(), size, safeSort);
    }
}
