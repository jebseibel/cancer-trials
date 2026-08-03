package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.Sponsor;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.service.SponsorDbService;
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
public class SponsorService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "createdAt", "updatedAt");

    private final SponsorDbService dbService;

    public SponsorService(SponsorDbService dbService) {
        super(Sponsor.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public Sponsor create(Sponsor item) {
        requireNonNull(item, "Sponsor");
        log.info("create(): {}", item);

        try {
            return dbService.create(item.getName(), item.getOrgClass());
        } catch (Exception e) {
            log.error("Failed to create sponsor: {}", item.getName(), e);
            throw new ServiceException("Unable to create sponsor", e);
        }
    }

    @Transactional
    public Sponsor update(String extid, Sponsor item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "Sponsor");
        log.info("update(): extid={}, {}", extid, item);

        try {
            Sponsor updated = dbService.update(extid, item.getName(), item.getOrgClass());
            if (updated == null) {
                throw new ResourceNotFoundException("Sponsor", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update sponsor: {}", extid, e);
            throw new ServiceException("Unable to update sponsor", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete sponsor: {}", extid, e);
            throw new ServiceException("Unable to delete sponsor", e);
        }
    }

    public Sponsor findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            Sponsor sponsor = dbService.findByExtid(extid);
            if (sponsor == null) {
                throw new ResourceNotFoundException("Sponsor", extid);
            }
            return sponsor;
        } catch (Exception e) {
            log.error("Failed to retrieve sponsor: {}", extid, e);
            throw new ServiceException("Unable to retrieve sponsor", e);
        }
    }

    public List<Sponsor> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all sponsors", e);
            throw new ServiceException("Unable to retrieve sponsors", e);
        }
    }

    public Optional<Sponsor> findByName(String name) {
        requireNonBlank(name, "name");
        log.info("findByName(): name={}", name);

        try {
            return Optional.ofNullable(dbService.findByName(name));
        } catch (Exception e) {
            log.error("Failed to retrieve sponsor by name: {}", name, e);
            throw new ServiceException("Unable to retrieve sponsor", e);
        }
    }

    @Transactional
    public Sponsor findOrCreateByName(String name, String orgClass) {
        requireNonBlank(name, "name");
        log.info("findOrCreateByName(): name={}", name);

        return findByName(name)
                .orElseGet(() -> dbService.create(name, orgClass));
    }

    public Page<Sponsor> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve sponsors (paged)", e);
            throw new ServiceException("Unable to retrieve sponsors", e);
        }
    }

    public List<Sponsor> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve sponsors by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve sponsors", e);
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
