package com.seibel.jobhunting.service;

import com.seibel.jobhunting.common.domain.Company;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.common.exceptions.ResourceNotFoundException;
import com.seibel.jobhunting.common.exceptions.ServiceException;
import com.seibel.jobhunting.database.db.service.CompanyDbService;
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
public class CompanyService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "createdAt", "updatedAt");

    private final CompanyDbService dbService;

    public CompanyService(CompanyDbService dbService) {
        super(Company.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public Company create(Company item) {
        requireNonNull(item, "Company");
        log.info("create(): {}", item);

        try {
            return dbService.create(item.getName(), item.getWebsite(), item.getIndustry(), item.getNotes());
        } catch (Exception e) {
            log.error("Failed to create company: {}", item.getName(), e);
            throw new ServiceException("Unable to create company", e);
        }
    }

    @Transactional
    public Company update(String extid, Company item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "Company");
        log.info("update(): extid={}, {}", extid, item);

        try {
            Company updated = dbService.update(extid, item.getName(), item.getWebsite(), item.getIndustry(), item.getNotes());
            if (updated == null) {
                throw new ResourceNotFoundException("Company", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update company: {}", extid, e);
            throw new ServiceException("Unable to update company", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete company: {}", extid, e);
            throw new ServiceException("Unable to delete company", e);
        }
    }

    public Company findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            Company company = dbService.findByExtid(extid);
            if (company == null) {
                throw new ResourceNotFoundException("Company", extid);
            }
            return company;
        } catch (Exception e) {
            log.error("Failed to retrieve company: {}", extid, e);
            throw new ServiceException("Unable to retrieve company", e);
        }
    }

    public List<Company> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all companies", e);
            throw new ServiceException("Unable to retrieve companies", e);
        }
    }

    public Page<Company> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve companies (paged)", e);
            throw new ServiceException("Unable to retrieve companies", e);
        }
    }

    public List<Company> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve companies by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve companies", e);
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
