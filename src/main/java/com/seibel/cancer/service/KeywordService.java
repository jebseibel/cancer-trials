package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.Keyword;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.service.KeywordDbService;
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
public class KeywordService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "createdAt", "updatedAt");

    private final KeywordDbService dbService;

    public KeywordService(KeywordDbService dbService) {
        super(Keyword.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public Keyword create(Keyword item) {
        requireNonNull(item, "Keyword");
        log.info("create(): {}", item);

        try {
            return dbService.create(item.getName());
        } catch (Exception e) {
            log.error("Failed to create keyword: {}", item.getName(), e);
            throw new ServiceException("Unable to create keyword", e);
        }
    }

    @Transactional
    public Keyword update(String extid, Keyword item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "Keyword");
        log.info("update(): extid={}, {}", extid, item);

        try {
            Keyword updated = dbService.update(extid, item.getName());
            if (updated == null) {
                throw new ResourceNotFoundException("Keyword", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update keyword: {}", extid, e);
            throw new ServiceException("Unable to update keyword", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete keyword: {}", extid, e);
            throw new ServiceException("Unable to delete keyword", e);
        }
    }

    public Keyword findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            Keyword keyword = dbService.findByExtid(extid);
            if (keyword == null) {
                throw new ResourceNotFoundException("Keyword", extid);
            }
            return keyword;
        } catch (Exception e) {
            log.error("Failed to retrieve keyword: {}", extid, e);
            throw new ServiceException("Unable to retrieve keyword", e);
        }
    }

    public List<Keyword> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all keywords", e);
            throw new ServiceException("Unable to retrieve keywords", e);
        }
    }

    public Page<Keyword> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve keywords (paged)", e);
            throw new ServiceException("Unable to retrieve keywords", e);
        }
    }

    public List<Keyword> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve keywords by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve keywords", e);
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
