package com.seibel.jobhunting.service;

import com.seibel.jobhunting.common.domain.FriendSkill;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.common.exceptions.ResourceNotFoundException;
import com.seibel.jobhunting.common.exceptions.ServiceException;
import com.seibel.jobhunting.database.db.service.FriendSkillDbService;
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
public class FriendSkillService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "updatedAt");

    private final FriendSkillDbService dbService;

    public FriendSkillService(FriendSkillDbService dbService) {
        super(FriendSkill.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public FriendSkill create(FriendSkill item) {
        requireNonNull(item, "FriendSkill");
        log.info("create(): {}", item);

        try {
            return dbService.create(item.getFriendId(), item.getSkillId());
        } catch (Exception e) {
            log.error("Failed to create friend skill: {}", item, e);
            throw new ServiceException("Unable to create friend skill", e);
        }
    }

    @Transactional
    public FriendSkill update(String extid, FriendSkill item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "FriendSkill");
        log.info("update(): extid={}, {}", extid, item);

        try {
            FriendSkill updated = dbService.update(extid, item.getFriendId(), item.getSkillId());
            if (updated == null) {
                throw new ResourceNotFoundException("FriendSkill", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update friend skill: {}", extid, e);
            throw new ServiceException("Unable to update friend skill", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete friend skill: {}", extid, e);
            throw new ServiceException("Unable to delete friend skill", e);
        }
    }

    public FriendSkill findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            FriendSkill friendSkill = dbService.findByExtid(extid);
            if (friendSkill == null) {
                throw new ResourceNotFoundException("FriendSkill", extid);
            }
            return friendSkill;
        } catch (Exception e) {
            log.error("Failed to retrieve friend skill: {}", extid, e);
            throw new ServiceException("Unable to retrieve friend skill", e);
        }
    }

    public List<FriendSkill> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all friend skills", e);
            throw new ServiceException("Unable to retrieve friend skills", e);
        }
    }

    public Page<FriendSkill> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve friend skills (paged)", e);
            throw new ServiceException("Unable to retrieve friend skills", e);
        }
    }

    public List<FriendSkill> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve friend skills by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve friend skills", e);
        }
    }

    public List<FriendSkill> findByFriendId(Long friendId) {
        requireNonNull(friendId, "friendId");
        log.info("findByFriendId(): friendId={}", friendId);

        try {
            return dbService.findByFriendId(friendId);
        } catch (Exception e) {
            log.error("Failed to retrieve friend skills by friendId: {}", friendId, e);
            throw new ServiceException("Unable to retrieve friend skills", e);
        }
    }

    public List<FriendSkill> findBySkillId(Long skillId) {
        requireNonNull(skillId, "skillId");
        log.info("findBySkillId(): skillId={}", skillId);

        try {
            return dbService.findBySkillId(skillId);
        } catch (Exception e) {
            log.error("Failed to retrieve friend skills by skillId: {}", skillId, e);
            throw new ServiceException("Unable to retrieve friend skills", e);
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
