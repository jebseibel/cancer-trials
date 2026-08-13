package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.PatientVariant;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.service.PatientVariantDbService;
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
public class PatientVariantService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "testDate", "createdAt", "updatedAt");

    private final PatientVariantDbService dbService;

    public PatientVariantService(PatientVariantDbService dbService) {
        super(PatientVariant.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public PatientVariant create(PatientVariant item) {
        requireNonNull(item, "PatientVariant");
        log.info("create(): patientId={}", item.getPatientId());

        try {
            return dbService.create(item);
        } catch (Exception e) {
            log.error("Failed to create patientVariant for patientId: {}", item.getPatientId(), e);
            throw new ServiceException("Unable to create patientVariant", e);
        }
    }

    @Transactional
    public PatientVariant update(String extid, PatientVariant item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "PatientVariant");
        log.info("update(): extid={}", extid);

        try {
            PatientVariant updated = dbService.update(extid, item);
            if (updated == null) {
                throw new ResourceNotFoundException("PatientVariant", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update patientVariant: {}", extid, e);
            throw new ServiceException("Unable to update patientVariant", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete patientVariant: {}", extid, e);
            throw new ServiceException("Unable to delete patientVariant", e);
        }
    }

    public PatientVariant findByExtid(String extid) {
        requireNonBlank(extid, "extid");

        try {
            PatientVariant found = dbService.findByExtid(extid);
            if (found == null) {
                throw new ResourceNotFoundException("PatientVariant", extid);
            }
            return found;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to find patientVariant: {}", extid, e);
            throw new ServiceException("Unable to find patientVariant", e);
        }
    }

    public List<PatientVariant> findAll() {
        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to find all patientVariants", e);
            throw new ServiceException("Unable to find patientVariants", e);
        }
    }

    public Page<PatientVariant> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);

        try {
            return activeEnum == null ? dbService.findAll(safe) : dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to find patientVariants page", e);
            throw new ServiceException("Unable to find patientVariants", e);
        }
    }

    public List<PatientVariant> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to find patientVariants by active: {}", activeEnum, e);
            throw new ServiceException("Unable to find patientVariants", e);
        }
    }

    /** The patient's variant row - the primary read for matching. */
    public List<PatientVariant> findByPatientId(Long patientId) {
        requireNonNull(patientId, "patientId");

        try {
            return dbService.findByPatientId(patientId);
        } catch (Exception e) {
            log.error("Failed to find patientVariants by patientId: {}", patientId, e);
            throw new ServiceException("Unable to find patientVariants", e);
        }
    }

    public List<PatientVariant> findByPatientDiagnosisId(Long patientDiagnosisId) {
        requireNonNull(patientDiagnosisId, "patientDiagnosisId");

        try {
            return dbService.findByPatientDiagnosisId(patientDiagnosisId);
        } catch (Exception e) {
            log.error("Failed to find patientVariants by patientDiagnosisId: {}", patientDiagnosisId, e);
            throw new ServiceException("Unable to find patientVariants", e);
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
