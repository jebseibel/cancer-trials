package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.Condition;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.service.ConditionDbService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ConditionService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "createdAt", "updatedAt");

    private final ConditionDbService dbService;

    public ConditionService(ConditionDbService dbService) {
        super(Condition.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public Condition create(Condition item) {
        requireNonNull(item, "Condition");
        log.info("create(): {}", item);

        try {
            return dbService.create(item.getName());
        } catch (Exception e) {
            log.error("Failed to create condition: {}", item.getName(), e);
            throw new ServiceException("Unable to create condition", e);
        }
    }

    @Transactional
    public Condition update(String extid, Condition item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "Condition");
        log.info("update(): extid={}, {}", extid, item);

        try {
            Condition updated = dbService.update(extid, item.getName());
            if (updated == null) {
                throw new ResourceNotFoundException("Condition", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update condition: {}", extid, e);
            throw new ServiceException("Unable to update condition", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete condition: {}", extid, e);
            throw new ServiceException("Unable to delete condition", e);
        }
    }

    public Condition findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            Condition condition = dbService.findByExtid(extid);
            if (condition == null) {
                throw new ResourceNotFoundException("Condition", extid);
            }
            return condition;
        } catch (Exception e) {
            log.error("Failed to retrieve condition: {}", extid, e);
            throw new ServiceException("Unable to retrieve condition", e);
        }
    }

    public List<Condition> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all conditions", e);
            throw new ServiceException("Unable to retrieve conditions", e);
        }
    }

    public Optional<Condition> findByName(String name) {
        requireNonBlank(name, "name");
        log.info("findByName(): name={}", name);

        try {
            return Optional.ofNullable(dbService.findByName(name));
        } catch (Exception e) {
            log.error("Failed to retrieve condition by name: {}", name, e);
            throw new ServiceException("Unable to retrieve condition", e);
        }
    }

    @Transactional
    public Condition findOrCreateByName(String name) {
        requireNonBlank(name, "name");
        log.info("findOrCreateByName(): name={}", name);

        return findByName(name)
                .orElseGet(() -> dbService.create(name));
    }

    public Page<Condition> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve conditions (paged)", e);
            throw new ServiceException("Unable to retrieve conditions", e);
        }
    }

    public List<Condition> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve conditions by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve conditions", e);
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
            safeSort = Sort.by(Sort.Order.asc("name"));
        }
        return PageRequest.of(pageable.getPageNumber(), size, safeSort);
    }
}
