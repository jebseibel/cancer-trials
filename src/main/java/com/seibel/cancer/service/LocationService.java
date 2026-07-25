package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.Location;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.service.LocationDbService;
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
public class LocationService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("facility", "city", "state", "createdAt", "updatedAt");

    private final LocationDbService dbService;

    public LocationService(LocationDbService dbService) {
        super(Location.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public Location create(Location item) {
        requireNonNull(item, "Location");
        log.info("create(): {}", item);

        try {
            return dbService.create(item);
        } catch (Exception e) {
            log.error("Failed to create location: {}", item, e);
            throw new ServiceException("Unable to create location", e);
        }
    }

    @Transactional
    public Location update(String extid, Location item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "Location");
        log.info("update(): extid={}, {}", extid, item);

        try {
            Location updated = dbService.update(extid, item);
            if (updated == null) {
                throw new ResourceNotFoundException("Location", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update location: {}", extid, e);
            throw new ServiceException("Unable to update location", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete location: {}", extid, e);
            throw new ServiceException("Unable to delete location", e);
        }
    }

    public Location findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            Location location = dbService.findByExtid(extid);
            if (location == null) {
                throw new ResourceNotFoundException("Location", extid);
            }
            return location;
        } catch (Exception e) {
            log.error("Failed to retrieve location: {}", extid, e);
            throw new ServiceException("Unable to retrieve location", e);
        }
    }

    public List<Location> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all locations", e);
            throw new ServiceException("Unable to retrieve locations", e);
        }
    }

    public Page<Location> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve locations (paged)", e);
            throw new ServiceException("Unable to retrieve locations", e);
        }
    }

    public List<Location> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve locations by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve locations", e);
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
            // If client requested only invalid fields, fall back to facility ASC
            safeSort = Sort.by(Sort.Order.asc("facility"));
        }
        return PageRequest.of(pageable.getPageNumber(), size, safeSort);
    }
}
