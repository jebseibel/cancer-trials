package com.seibel.cancer.service;

import com.seibel.cancer.common.domain.PatientMedication;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ResourceNotFoundException;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.service.PatientMedicationDbService;
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
public class PatientMedicationService extends BaseService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "medicationName", "status", "authoredOn", "createdAt", "updatedAt");

    private final PatientMedicationDbService dbService;

    public PatientMedicationService(PatientMedicationDbService dbService) {
        super(PatientMedication.class.getSimpleName());
        this.dbService = dbService;
    }

    @Transactional
    public PatientMedication create(PatientMedication item) {
        requireNonNull(item, "PatientMedication");
        log.info("create(): {}", item.getFhirResourceId());

        try {
            return dbService.create(item);
        } catch (Exception e) {
            log.error("Failed to create patientMedication: {}", item.getFhirResourceId(), e);
            throw new ServiceException("Unable to create patientMedication", e);
        }
    }

    @Transactional
    public PatientMedication update(String extid, PatientMedication item) {
        requireNonBlank(extid, "extid");
        requireNonNull(item, "PatientMedication");
        log.info("update(): extid={}", extid);

        try {
            PatientMedication updated = dbService.update(extid, item);
            if (updated == null) {
                throw new ResourceNotFoundException("PatientMedication", extid);
            }
            return updated;
        } catch (Exception e) {
            log.error("Failed to update patientMedication: {}", extid, e);
            throw new ServiceException("Unable to update patientMedication", e);
        }
    }

    @Transactional
    public boolean delete(String extid) {
        requireNonBlank(extid, "extid");
        log.info("delete(): extid={}", extid);

        try {
            return dbService.delete(extid);
        } catch (Exception e) {
            log.error("Failed to delete patientMedication: {}", extid, e);
            throw new ServiceException("Unable to delete patientMedication", e);
        }
    }

    public PatientMedication findByExtid(String extid) {
        requireNonBlank(extid, "extid");
        log.info("findByExtid(): extid={}", extid);

        try {
            PatientMedication patientMedication = dbService.findByExtid(extid);
            if (patientMedication == null) {
                throw new ResourceNotFoundException("PatientMedication", extid);
            }
            return patientMedication;
        } catch (Exception e) {
            log.error("Failed to retrieve patientMedication: {}", extid, e);
            throw new ServiceException("Unable to retrieve patientMedication", e);
        }
    }

    /** Lookup by Epic's own resource id - used by ingestion to dedup before insert. */
    public PatientMedication findByFhirResourceId(String fhirResourceId) {
        requireNonBlank(fhirResourceId, "fhirResourceId");
        log.info("findByFhirResourceId(): fhirResourceId={}", fhirResourceId);

        try {
            return dbService.findByFhirResourceId(fhirResourceId);
        } catch (Exception e) {
            log.error("Failed to retrieve patientMedication by fhirResourceId: {}", fhirResourceId, e);
            throw new ServiceException("Unable to retrieve patientMedication", e);
        }
    }

    public List<PatientMedication> findByStatus(String status) {
        requireNonBlank(status, "status");
        log.info("findByStatus(): status={}", status);

        try {
            return dbService.findByStatus(status);
        } catch (Exception e) {
            log.error("Failed to retrieve patientMedications by status: {}", status, e);
            throw new ServiceException("Unable to retrieve patientMedications", e);
        }
    }

    public List<PatientMedication> findAll() {
        log.info("findAll()");

        try {
            return dbService.findAll();
        } catch (Exception e) {
            log.error("Failed to retrieve all patientMedications", e);
            throw new ServiceException("Unable to retrieve patientMedications", e);
        }
    }

    public Page<PatientMedication> findAll(Pageable pageable, ActiveEnum activeEnum) {
        Pageable safe = enforceCapsAndWhitelist(pageable);
        log.info("findAll(pageable): page={}, size={}, sort={}", safe.getPageNumber(), safe.getPageSize(), safe.getSort());
        try {
            if (activeEnum == null) {
                return dbService.findAll(safe);
            }
            return dbService.findByActive(activeEnum, safe);
        } catch (Exception e) {
            log.error("Failed to retrieve patientMedications (paged)", e);
            throw new ServiceException("Unable to retrieve patientMedications", e);
        }
    }

    public List<PatientMedication> findByActive(ActiveEnum activeEnum) {
        requireNonNull(activeEnum, "activeEnum");
        log.info("findByActive(): activeEnum={}", activeEnum);

        try {
            return dbService.findByActive(activeEnum);
        } catch (Exception e) {
            log.error("Failed to retrieve patientMedications by active status: {}", activeEnum, e);
            throw new ServiceException("Unable to retrieve patientMedications", e);
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
            // If client requested only invalid fields, fall back to medicationName ASC
            safeSort = Sort.by(Sort.Order.asc("medicationName"));
        }
        return PageRequest.of(pageable.getPageNumber(), size, safeSort);
    }
}
