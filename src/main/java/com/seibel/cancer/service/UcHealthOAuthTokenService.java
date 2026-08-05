package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.UcHealthOAuthToken;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.service.UcHealthOAuthTokenDbService;
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
public class UcHealthOAuthTokenService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("expiresAt", "createdAt", "updatedAt");

    private final UcHealthOAuthTokenDbService dbService;

    public UcHealthOAuthTokenService(UcHealthOAuthTokenDbService dbService) {
        super(UcHealthOAuthToken.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public UcHealthOAuthToken create(UcHealthOAuthToken item) {
        requireNonNull(item, "UcHealthOAuthToken");
        log.info("create()");

        try {
            return dbService.create(item.getAccessToken(), item.getRefreshToken(), item.getExpiresAt(),
                    item.getPatientFhirId(), item.getScope());
        } catch (Exception e) {
            log.error("Failed to create ucHealthOAuthToken", e);
            throw new ServiceException("Unable to create ucHealthOAuthToken", e);
        }
    }

    @Transactional
    public UcHealthOAuthToken update(String extid, UcHealthOAuthToken item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "UcHealthOAuthToken");
        log.info("update(): extid={}", extid);

        try {
            UcHealthOAuthToken updated = dbService.update(extid, item.getAccessToken(), item.getRefreshToken(),
                    item.getExpiresAt(), item.getPatientFhirId(), item.getScope());
            if (updated == null) {
                throw new ResourceNotFoundException("UcHealthOAuthToken", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update ucHealthOAuthToken: {}", extid, e);
            throw new ServiceException("Unable to update ucHealthOAuthToken", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete ucHealthOAuthToken: {}", extid, e);
            throw new ServiceException("Unable to delete ucHealthOAuthToken", e);
        }
    }

    public UcHealthOAuthToken findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            UcHealthOAuthToken ucHealthOAuthToken = dbService.findByExtid(extid);
            if (ucHealthOAuthToken == null) {
                throw new ResourceNotFoundException("UcHealthOAuthToken", extid);
            }
            return ucHealthOAuthToken;
        } catch (Exception e) {
            log.error("Failed to retrieve ucHealthOAuthToken: {}", extid, e);
            throw new ServiceException("Unable to retrieve ucHealthOAuthToken", e);
        }
    }

    public UcHealthOAuthToken findCurrent() {
        log.info("findCurrent()");

        try {
            return dbService.findCurrent();
        } catch (Exception e) {
            log.error("Failed to retrieve current ucHealthOAuthToken", e);
            throw new ServiceException("Unable to retrieve current ucHealthOAuthToken", e);
        }
    }

    public List<UcHealthOAuthToken> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all ucHealthOAuthTokens", e);
            throw new ServiceException("Unable to retrieve ucHealthOAuthTokens", e);
        }
    }

    public Page<UcHealthOAuthToken> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve ucHealthOAuthTokens (paged)", e);
            throw new ServiceException("Unable to retrieve ucHealthOAuthTokens", e);
        }
    }

    public List<UcHealthOAuthToken> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve ucHealthOAuthTokens by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve ucHealthOAuthTokens", e);
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
