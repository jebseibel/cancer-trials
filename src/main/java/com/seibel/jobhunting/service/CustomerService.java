package com.seibel.jobhunting.service;

import com.seibel.jobhunting.common.domain.Customer;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.common.exceptions.ResourceNotFoundException;
import com.seibel.jobhunting.common.exceptions.ServiceException;
import com.seibel.jobhunting.database.db.service.CustomerDbService;
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
public class CustomerService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "code", "createdAt", "updatedAt");

    private final CustomerDbService dbService;

    public CustomerService(CustomerDbService dbService) {
        super(Customer.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public Customer create(Customer item) {
        requireNonNull(item, "Customer");
        log.info("create(): {}", item);

        try {
            return dbService.create(item.getCode(), item.getName(), item.getContactName(), item.getDescription(), item.getContactEmail(), item.getContactPhone());
        } catch (Exception e) {
            log.error("Failed to create customer: {}", item.getCode(), e);
            throw new ServiceException("Unable to create customer", e);
        }
    }

    @Transactional
    public Customer update(String extid, Customer item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "Customer");
        log.info("update(): extid={}, {}", extid, item);

        try {
            Customer updated = dbService.update(extid, item.getCode(), item.getName(), item.getContactName(), item.getDescription(), item.getContactEmail(), item.getContactPhone());
            if (updated == null) {
                throw new ResourceNotFoundException("Customer", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update customer: {}", extid, e);
            throw new ServiceException("Unable to update customer", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete customer: {}", extid, e);
            throw new ServiceException("Unable to delete customer", e);
        }
    }

    public Customer findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            Customer customer = dbService.findByExtid(extid);
            if (customer == null) {
                throw new ResourceNotFoundException("Customer", extid);
            }
            return customer;
        } catch (Exception e) {
            log.error("Failed to retrieve customer: {}", extid, e);
            throw new ServiceException("Unable to retrieve customer", e);
        }
    }

    public List<Customer> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all customers", e);
            throw new ServiceException("Unable to retrieve customers", e);
        }
    }

    public Page<Customer> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve customers (paged)", e);
            throw new ServiceException("Unable to retrieve customers", e);
        }
    }

    public List<Customer> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve customers by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve customers", e);
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
