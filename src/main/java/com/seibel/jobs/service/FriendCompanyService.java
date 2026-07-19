package com.seibel.jobs.service;

import com.seibel.jobs.common.domain.FriendCompany;
import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.common.exceptions.ResourceNotFoundException;
import com.seibel.jobs.common.exceptions.ServiceException;
import com.seibel.jobs.database.db.service.FriendCompanyDbService;
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
public class FriendCompanyService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "updatedAt");

    private final FriendCompanyDbService dbService;

    public FriendCompanyService(FriendCompanyDbService dbService) {
        super(FriendCompany.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public FriendCompany create(FriendCompany item) {
        requireNonNull(item, "FriendCompany");
        log.info("create(): {}", item);

        try {
            return dbService.create(item.getFriendId(), item.getCompanyId());
        } catch (Exception e) {
            log.error("Failed to create friend company: {}", item, e);
            throw new ServiceException("Unable to create friend company", e);
        }
    }

    @Transactional
    public FriendCompany update(String extid, FriendCompany item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "FriendCompany");
        log.info("update(): extid={}, {}", extid, item);

        try {
            FriendCompany updated = dbService.update(extid, item.getFriendId(), item.getCompanyId());
            if (updated == null) {
                throw new ResourceNotFoundException("FriendCompany", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update friend company: {}", extid, e);
            throw new ServiceException("Unable to update friend company", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete friend company: {}", extid, e);
            throw new ServiceException("Unable to delete friend company", e);
        }
    }

    public FriendCompany findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            FriendCompany friendCompany = dbService.findByExtid(extid);
            if (friendCompany == null) {
                throw new ResourceNotFoundException("FriendCompany", extid);
            }
            return friendCompany;
        } catch (Exception e) {
            log.error("Failed to retrieve friend company: {}", extid, e);
            throw new ServiceException("Unable to retrieve friend company", e);
        }
    }

    public List<FriendCompany> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all friend companies", e);
            throw new ServiceException("Unable to retrieve friend companies", e);
        }
    }

    public Page<FriendCompany> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve friend companies (paged)", e);
            throw new ServiceException("Unable to retrieve friend companies", e);
        }
    }

    public List<FriendCompany> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve friend companies by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve friend companies", e);
        }
    }

    public List<FriendCompany> findByFriendId(Long friendId) {
        requireNonNull(friendId, "friendId");
        log.info("findByFriendId(): friendId={}", friendId);

        try {
            return dbService.findByFriendId(friendId);
        } catch (Exception e) {
            log.error("Failed to retrieve friend companies by friendId: {}", friendId, e);
            throw new ServiceException("Unable to retrieve friend companies", e);
        }
    }

    public List<FriendCompany> findByCompanyId(Long companyId) {
        requireNonNull(companyId, "companyId");
        log.info("findByCompanyId(): companyId={}", companyId);

        try {
            return dbService.findByCompanyId(companyId);
        } catch (Exception e) {
            log.error("Failed to retrieve friend companies by companyId: {}", companyId, e);
            throw new ServiceException("Unable to retrieve friend companies", e);
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
