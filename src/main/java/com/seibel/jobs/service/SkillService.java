package com.seibel.jobs.service;

import com.seibel.jobs.common.domain.Skill;
import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.common.exceptions.ResourceNotFoundException;
import com.seibel.jobs.common.exceptions.ServiceException;
import com.seibel.jobs.database.db.service.SkillDbService;
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
public class SkillService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "createdAt", "updatedAt");

    private final SkillDbService dbService;

    public SkillService(SkillDbService dbService) {
        super(Skill.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public Skill create(Skill item) {
        requireNonNull(item, "Skill");
        log.info("create(): {}", item);

        try {
            return dbService.create(item.getName());
        } catch (Exception e) {
            log.error("Failed to create skill: {}", item.getName(), e);
            throw new ServiceException("Unable to create skill", e);
        }
    }

    @Transactional
    public Skill update(String extid, Skill item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "Skill");
        log.info("update(): extid={}, {}", extid, item);

        try {
            Skill updated = dbService.update(extid, item.getName());
            if (updated == null) {
                throw new ResourceNotFoundException("Skill", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update skill: {}", extid, e);
            throw new ServiceException("Unable to update skill", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete skill: {}", extid, e);
            throw new ServiceException("Unable to delete skill", e);
        }
    }

    public Skill findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            Skill skill = dbService.findByExtid(extid);
            if (skill == null) {
                throw new ResourceNotFoundException("Skill", extid);
            }
            return skill;
        } catch (Exception e) {
            log.error("Failed to retrieve skill: {}", extid, e);
            throw new ServiceException("Unable to retrieve skill", e);
        }
    }

    public List<Skill> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all skills", e);
            throw new ServiceException("Unable to retrieve skills", e);
        }
    }

    public Page<Skill> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve skills (paged)", e);
            throw new ServiceException("Unable to retrieve skills", e);
        }
    }

    public List<Skill> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve skills by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve skills", e);
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
