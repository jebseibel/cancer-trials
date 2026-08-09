package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.PatientPriorTreatment;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.service.PatientPriorTreatmentDbService;
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
public class PatientPriorTreatmentService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "lastTreatmentEndDate", "createdAt", "updatedAt");

    private final PatientPriorTreatmentDbService dbService;

    public PatientPriorTreatmentService(PatientPriorTreatmentDbService dbService) {
        super(PatientPriorTreatment.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public PatientPriorTreatment create(PatientPriorTreatment item) {
        requireNonNull(item, "PatientPriorTreatment");
        log.info("create(): appUserId={}", item.getAppUserId());

        try {
            return dbService.create(item);
        } catch (Exception e) {
            log.error("Failed to create patientPriorTreatment for appUserId: {}", item.getAppUserId(), e);
            throw new ServiceException("Unable to create patientPriorTreatment", e);
        }
    }

    @Transactional
    public PatientPriorTreatment update(String extid, PatientPriorTreatment item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "PatientPriorTreatment");
        log.info("update(): extid={}", extid);

        try {
            PatientPriorTreatment updated = dbService.update(extid, item);
            if (updated == null) {
                throw new ResourceNotFoundException("PatientPriorTreatment", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update patientPriorTreatment: {}", extid, e);
            throw new ServiceException("Unable to update patientPriorTreatment", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete patientPriorTreatment: {}", extid, e);
            throw new ServiceException("Unable to delete patientPriorTreatment", e);
        }
    }

    public PatientPriorTreatment findByExtid(String extid) {
        requireNonBlank(extid, "extid");

        try {
            PatientPriorTreatment found = dbService.findByExtid(extid);
            if (found == null) {
                throw new ResourceNotFoundException("PatientPriorTreatment", extid);
            }
            return found;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to find patientPriorTreatment: {}", extid, e);
            throw new ServiceException("Unable to find patientPriorTreatment", e);
        }
    }

    public List<PatientPriorTreatment> findAll() {
        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to find all patientPriorTreatments", e);
            throw new ServiceException("Unable to find patientPriorTreatments", e);
        }
    }

    public Page<PatientPriorTreatment> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);

        try {
            return activeEnum == null ? dbService.findAll(safe) : dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to find patientPriorTreatments page", e);
            throw new ServiceException("Unable to find patientPriorTreatments", e);
        }
    }

    public List<PatientPriorTreatment> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to find patientPriorTreatments by active: {}", activeEnum, e);
            throw new ServiceException("Unable to find patientPriorTreatments", e);
        }
    }

    /** The patient's treatment row - the primary read for matching. */
    public List<PatientPriorTreatment> findByAppUserId(Long appUserId) {
        requireNonNull(appUserId, "appUserId");

        try {
            return dbService.findByAppUserId(appUserId);
        } catch (Exception e) {
            log.error("Failed to find patientPriorTreatments by appUserId: {}", appUserId, e);
            throw new ServiceException("Unable to find patientPriorTreatments", e);
        }
    }

    public List<PatientPriorTreatment> findByPatientDiagnosisId(Long patientDiagnosisId) {
        requireNonNull(patientDiagnosisId, "patientDiagnosisId");

        try {
            return dbService.findByPatientDiagnosisId(patientDiagnosisId);
        } catch (Exception e) {
            log.error("Failed to find patientPriorTreatments by patientDiagnosisId: {}", patientDiagnosisId, e);
            throw new ServiceException("Unable to find patientPriorTreatments", e);
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
