package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.PatientDiagnosis;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.service.PatientDiagnosisDbService;
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
public class PatientDiagnosisService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "cancerType", "diagnosisDate", "createdAt", "updatedAt");

    private final PatientDiagnosisDbService dbService;

    public PatientDiagnosisService(PatientDiagnosisDbService dbService) {
        super(PatientDiagnosis.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public PatientDiagnosis create(PatientDiagnosis item) {
        requireNonNull(item, "PatientDiagnosis");
        log.info("create(): {}", item.getCancerType());

        try {
            return dbService.create(item);
        } catch (Exception e) {
            log.error("Failed to create patientDiagnosis: {}", item.getCancerType(), e);
            throw new ServiceException("Unable to create patientDiagnosis", e);
        }
    }

    @Transactional
    public PatientDiagnosis update(String extid, PatientDiagnosis item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "PatientDiagnosis");
        log.info("update(): extid={}", extid);

        try {
            PatientDiagnosis updated = dbService.update(extid, item);
            if (updated == null) {
                throw new ResourceNotFoundException("PatientDiagnosis", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update patientDiagnosis: {}", extid, e);
            throw new ServiceException("Unable to update patientDiagnosis", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete patientDiagnosis: {}", extid, e);
            throw new ServiceException("Unable to delete patientDiagnosis", e);
        }
    }

    public PatientDiagnosis findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            PatientDiagnosis patientDiagnosis = dbService.findByExtid(extid);
            if (patientDiagnosis == null) {
                throw new ResourceNotFoundException("PatientDiagnosis", extid);
            }
            return patientDiagnosis;
        } catch (Exception e) {
            log.error("Failed to retrieve patientDiagnosis: {}", extid, e);
            throw new ServiceException("Unable to retrieve patientDiagnosis", e);
        }
    }

    /** One patient's diagnosis, looked up by the owning app user. */
    public List<PatientDiagnosis> findByPatientId(Long patientId) {
        requireNonNull(patientId, "patientId");
        log.info("findByPatientId(): patientId={}", patientId);

        try {
            return dbService.findByPatientId(patientId);
        } catch (Exception e) {
            log.error("Failed to retrieve patientDiagnoses by patientId: {}", patientId, e);
            throw new ServiceException("Unable to retrieve patientDiagnoses", e);
        }
    }

    public List<PatientDiagnosis> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all patientDiagnoses", e);
            throw new ServiceException("Unable to retrieve patientDiagnoses", e);
        }
    }

    public Page<PatientDiagnosis> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve patientDiagnoses (paged)", e);
            throw new ServiceException("Unable to retrieve patientDiagnoses", e);
        }
    }

    public List<PatientDiagnosis> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve patientDiagnoses by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve patientDiagnoses", e);
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
            // If client requested only invalid fields, fall back to newest diagnosis first
            safeSort = Sort.by(Sort.Order.desc("createdAt"));
        }
        return PageRequest.of(pageable.getPageNumber(), size, safeSort);
    }
}
