package com.seibel.jobs.service;

import com.seibel.jobs.common.domain.Contact;
import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.common.exceptions.ResourceNotFoundException;
import com.seibel.jobs.common.exceptions.ServiceException;
import com.seibel.jobs.database.db.service.ContactDbService;
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
public class ContactService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "createdAt", "updatedAt");

    private final ContactDbService dbService;

    public ContactService(ContactDbService dbService) {
        super(Contact.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public Contact create(Contact item) {
        requireNonNull(item, "Contact");
        log.info("create(): {}", item);

        try {
            return dbService.create(item.getCompanyId(), item.getJobPostingId(), item.getName(), item.getRole(), item.getEmail(), item.getPhone(), item.getNotes());
        } catch (Exception e) {
            log.error("Failed to create contact: {}", item.getName(), e);
            throw new ServiceException("Unable to create contact", e);
        }
    }

    @Transactional
    public Contact update(String extid, Contact item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "Contact");
        log.info("update(): extid={}, {}", extid, item);

        try {
            Contact updated = dbService.update(extid, item.getCompanyId(), item.getJobPostingId(), item.getName(), item.getRole(), item.getEmail(), item.getPhone(), item.getNotes());
            if (updated == null) {
                throw new ResourceNotFoundException("Contact", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update contact: {}", extid, e);
            throw new ServiceException("Unable to update contact", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete contact: {}", extid, e);
            throw new ServiceException("Unable to delete contact", e);
        }
    }

    public Contact findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            Contact contact = dbService.findByExtid(extid);
            if (contact == null) {
                throw new ResourceNotFoundException("Contact", extid);
            }
            return contact;
        } catch (Exception e) {
            log.error("Failed to retrieve contact: {}", extid, e);
            throw new ServiceException("Unable to retrieve contact", e);
        }
    }

    public List<Contact> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all contacts", e);
            throw new ServiceException("Unable to retrieve contacts", e);
        }
    }

    public Page<Contact> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve contacts (paged)", e);
            throw new ServiceException("Unable to retrieve contacts", e);
        }
    }

    public List<Contact> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve contacts by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve contacts", e);
        }
    }

    public List<Contact> findByCompanyId(Long companyId) {
        requireNonNull(companyId, "companyId");
        log.info("findByCompanyId(): companyId={}", companyId);

        try {
            return dbService.findByCompanyId(companyId);
        } catch (Exception e) {
            log.error("Failed to retrieve contacts by companyId: {}", companyId, e);
            throw new ServiceException("Unable to retrieve contacts", e);
        }
    }

    public List<Contact> findByJobPostingId(Long jobPostingId) {
        requireNonNull(jobPostingId, "jobPostingId");
        log.info("findByJobPostingId(): jobPostingId={}", jobPostingId);

        try {
            return dbService.findByJobPostingId(jobPostingId);
        } catch (Exception e) {
            log.error("Failed to retrieve contacts by jobPostingId: {}", jobPostingId, e);
            throw new ServiceException("Unable to retrieve contacts", e);
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
