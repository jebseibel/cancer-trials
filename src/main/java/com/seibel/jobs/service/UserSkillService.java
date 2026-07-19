package com.seibel.jobs.service;

import com.seibel.jobs.common.domain.UserSkill;
import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.common.exceptions.ResourceNotFoundException;
import com.seibel.jobs.common.exceptions.ServiceException;
import com.seibel.jobs.database.db.service.UserSkillDbService;
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
public class UserSkillService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "updatedAt");

    private final UserSkillDbService dbService;

    public UserSkillService(UserSkillDbService dbService) {
        super(UserSkill.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public UserSkill create(UserSkill item) {
        requireNonNull(item, "UserSkill");
        log.info("create(): {}", item);

        try {
            return dbService.create(item.getUserId(), item.getSkillId());
        } catch (Exception e) {
            log.error("Failed to create user skill: {}", item, e);
            throw new ServiceException("Unable to create user skill", e);
        }
    }

    @Transactional
    public UserSkill update(String extid, UserSkill item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "UserSkill");
        log.info("update(): extid={}, {}", extid, item);

        try {
            UserSkill updated = dbService.update(extid, item.getUserId(), item.getSkillId());
            if (updated == null) {
                throw new ResourceNotFoundException("UserSkill", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update user skill: {}", extid, e);
            throw new ServiceException("Unable to update user skill", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete user skill: {}", extid, e);
            throw new ServiceException("Unable to delete user skill", e);
        }
    }

    public UserSkill findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            UserSkill userSkill = dbService.findByExtid(extid);
            if (userSkill == null) {
                throw new ResourceNotFoundException("UserSkill", extid);
            }
            return userSkill;
        } catch (Exception e) {
            log.error("Failed to retrieve user skill: {}", extid, e);
            throw new ServiceException("Unable to retrieve user skill", e);
        }
    }

    public List<UserSkill> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all user skills", e);
            throw new ServiceException("Unable to retrieve user skills", e);
        }
    }

    public Page<UserSkill> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve user skills (paged)", e);
            throw new ServiceException("Unable to retrieve user skills", e);
        }
    }

    public List<UserSkill> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve user skills by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve user skills", e);
        }
    }

    public List<UserSkill> findByUserId(Long userId) {
        requireNonNull(userId, "userId");
        log.info("findByUserId(): userId={}", userId);

        try {
            return dbService.findByUserId(userId);
        } catch (Exception e) {
            log.error("Failed to retrieve user skills by userId: {}", userId, e);
            throw new ServiceException("Unable to retrieve user skills", e);
        }
    }

    public List<UserSkill> findBySkillId(Long skillId) {
        requireNonNull(skillId, "skillId");
        log.info("findBySkillId(): skillId={}", skillId);

        try {
            return dbService.findBySkillId(skillId);
        } catch (Exception e) {
            log.error("Failed to retrieve user skills by skillId: {}", skillId, e);
            throw new ServiceException("Unable to retrieve user skills", e);
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
