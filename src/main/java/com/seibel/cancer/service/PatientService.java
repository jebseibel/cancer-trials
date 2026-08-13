package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.Patient;
import com.seibel.cancer.common.enums.AccessLevel;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.entity.UserDb;
import com.seibel.cancer.database.db.service.PatientDbService;
import com.seibel.cancer.database.db.service.UserPatientDbService;
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
public class PatientService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "displayName", "createdAt", "updatedAt");

    private final PatientDbService dbService;
    private final UserPatientDbService userPatientDbService;
    private final CurrentUserService currentUserService;

    public PatientService(PatientDbService dbService,
                          UserPatientDbService userPatientDbService,
                          CurrentUserService currentUserService) {
        super(Patient.class.getSimpleName());
        this.dbService = dbService;
        this.userPatientDbService = userPatientDbService;
        this.currentUserService = currentUserService;
    }

    /**
     * Create a patient and grant its creator OWNER, in one transaction.
     *
     * <p><strong>The two writes must not be separable.</strong> A patient with no grant is
     * unreachable by everyone including the person who just created it - there is no
     * "unowned" state that any endpoint can recover from, since every read goes through a
     * grant lookup. Creating the row and granting access are one operation.
     */
    @Transactional
    public Patient createOwnedByCurrentUser(Patient item) {
        requireNonNull(item, "Patient");
        UserDb owner = currentUserService.requireCurrentUser();

        Patient created = create(item);

        userPatientDbService.grant(owner.getId(), created.getId(), AccessLevel.OWNER,
                owner.getId(), null);
        log.info("createOwnedByCurrentUser(): patient extid={} owned by user={}",
                created.getExtid(), owner.getUsername());

        return created;
    }

    @Transactional
    public Patient create(Patient item) {
        requireNonNull(item, "Patient");
        log.info("create(): displayName={}", item.getDisplayName());

        try {
            return dbService.create(item);
        } catch (Exception e) {
            log.error("Failed to create patient: {}", item.getDisplayName(), e);
            throw new ServiceException("Unable to create patient", e);
        }
    }

    @Transactional
    public Patient update(String extid, Patient item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "Patient");
        log.info("update(): extid={}", extid);

        try {
            Patient updated = dbService.update(extid, item);
            if (updated == null) {
                throw new ResourceNotFoundException("Patient", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update patient: {}", extid, e);
            throw new ServiceException("Unable to update patient", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete patient: {}", extid, e);
            throw new ServiceException("Unable to delete patient", e);
        }
    }

    public Patient findByExtid(String extid) {
        requireNonBlank(extid, "extid");

        try {
            Patient found = dbService.findByExtid(extid);
            if (found == null) {
                throw new ResourceNotFoundException("Patient", extid);
            }
            return found;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to find patient: {}", extid, e);
            throw new ServiceException("Unable to find patient", e);
        }
    }

    public List<Patient> findAll() {
        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to find all patients", e);
            throw new ServiceException("Unable to find patients", e);
        }
    }

    public Page<Patient> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);

        try {
            return activeEnum == null ? dbService.findAll(safe) : dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to find patients page", e);
            throw new ServiceException("Unable to find patients", e);
        }
    }

    public List<Patient> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to find patients by active: {}", activeEnum, e);
            throw new ServiceException("Unable to find patients", e);
        }
    }

    private Pageable enforceCapsAndWhitelist(Pageable pageable) {
        if (pageable == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        int size = Math.min(pageable.getPageSize(), MAX_PAGE_SIZE);

        Sort safeSort = Sort.by(pageable.getSort().stream()
                .filter(order -> ALLOWED_SORT_FIELDS.contains(order.getProperty()))
                .toList());

        if (safeSort.isEmpty()) {
            safeSort = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        return PageRequest.of(pageable.getPageNumber(), size, safeSort);
    }
}
