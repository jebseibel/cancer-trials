package com.seibel.jobhunting.service;

import com.seibel.jobhunting.common.domain.Purchase;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.common.exceptions.ResourceNotFoundException;
import com.seibel.jobhunting.common.exceptions.ServiceException;
import com.seibel.jobhunting.database.db.service.PurchaseDbService;
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
public class PurchaseService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("customer", "status", "createdAt", "updatedAt");

    private final PurchaseDbService dbService;

    public PurchaseService(PurchaseDbService dbService) {
        super(Purchase.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public Purchase create(Purchase item) {
        requireNonNull(item, "Purchase");
        log.info("create(): {}", item);

        try {
            return dbService.create(item.getCustomer(), item.getItems(), item.getStatus());
        } catch (Exception e) {
            log.error("Failed to create purchase: {}", item.getCustomer(), e);
            throw new ServiceException("Unable to create purchase", e);
        }
    }

    @Transactional
    public Purchase update(String extid, Purchase item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "Purchase");
        log.info("update(): extid={}, {}", extid, item);

        try {
            Purchase updated = dbService.update(extid, item.getCustomer(), item.getItems(), item.getStatus());
            if (updated == null) {
                throw new ResourceNotFoundException("Purchase", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update purchase: {}", extid, e);
            throw new ServiceException("Unable to update purchase", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete purchase: {}", extid, e);
            throw new ServiceException("Unable to delete purchase", e);
        }
    }

    public Purchase findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            Purchase purchase = dbService.findByExtid(extid);
            if (purchase == null) {
                throw new ResourceNotFoundException("Purchase", extid);
            }
            return purchase;
        } catch (Exception e) {
            log.error("Failed to retrieve purchase: {}", extid, e);
            throw new ServiceException("Unable to retrieve purchase", e);
        }
    }

    public List<Purchase> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all purchases", e);
            throw new ServiceException("Unable to retrieve purchases", e);
        }
    }

    public Page<Purchase> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve purchases (paged)", e);
            throw new ServiceException("Unable to retrieve purchases", e);
        }
    }

    public List<Purchase> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve purchases by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve purchases", e);
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
            // If client requested only invalid fields, fall back to customer ASC
            safeSort = Sort.by(Sort.Order.asc("customer"));
        }
        return PageRequest.of(pageable.getPageNumber(), size, safeSort);
    }
}
