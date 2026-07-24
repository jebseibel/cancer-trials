package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.User;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.service.UserDbService;
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
public class UserService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("username", "email", "role", "createdAt", "updatedAt");

    private final UserDbService dbService;

    public UserService(UserDbService dbService) {
        super(User.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public User create(User item) {
        requireNonNull(item, "User");
        log.info("create(): {}", item);

        try {
            return dbService.create(item.getUsername(), item.getPassword(), item.getEmail(), item.getRole());
        } catch (Exception e) {
            log.error("Failed to create user: {}", item.getUsername(), e);
            throw new ServiceException("Unable to create user", e);
        }
    }

    @Transactional
    public User update(String extid, User item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "User");
        log.info("update(): extid={}, {}", extid, item);

        try {
            User updated = dbService.update(extid, item.getUsername(), item.getPassword(), item.getEmail(), item.getRole());
            if (updated == null) {
                throw new ResourceNotFoundException("User", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update user: {}", extid, e);
            throw new ServiceException("Unable to update user", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete user: {}", extid, e);
            throw new ServiceException("Unable to delete user", e);
        }
    }

    public User findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            User user = dbService.findByExtid(extid);
            if (user == null) {
                throw new ResourceNotFoundException("User", extid);
            }
            return user;
        } catch (Exception e) {
            log.error("Failed to retrieve user: {}", extid, e);
            throw new ServiceException("Unable to retrieve user", e);
        }
    }

    public List<User> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all users", e);
            throw new ServiceException("Unable to retrieve users", e);
        }
    }

    public Page<User> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve users (paged)", e);
            throw new ServiceException("Unable to retrieve users", e);
        }
    }

    public List<User> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve users by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve users", e);
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
            // If client requested only invalid fields, fall back to username ASC
            safeSort = Sort.by(Sort.Order.asc("username"));
        }
        return PageRequest.of(pageable.getPageNumber(), size, safeSort);
    }
}
