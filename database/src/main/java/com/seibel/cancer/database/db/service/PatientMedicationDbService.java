package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.PatientMedication;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.entity.PatientMedicationDb;
import com.seibel.cancer.database.db.mapper.PatientMedicationMapper;
import com.seibel.cancer.database.db.repository.PatientMedicationRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class PatientMedicationDbService extends BaseDbService {

    private final PatientMedicationRepository repository;
    private final PatientMedicationMapper mapper;

    public PatientMedicationDbService(PatientMedicationRepository repository, PatientMedicationMapper mapper) {
        super("PatientMedicationDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    // No positional-field create/update overload here on purpose: with 17 fields (11 of them
    // adjacent Strings) a positional call is too easy to transpose silently. Domain-object only.

    public PatientMedication create(@NonNull PatientMedication item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            PatientMedicationDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            PatientMedicationDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public PatientMedication update(@NonNull String extid, @NonNull PatientMedication item) {

        PatientMedicationDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (item.getFhirResourceId() != null) record.setFhirResourceId(item.getFhirResourceId());
            if (item.getMedicationName() != null) record.setMedicationName(item.getMedicationName());
            if (item.getRxnormCode() != null) record.setRxnormCode(item.getRxnormCode());
            if (item.getStatus() != null) record.setStatus(item.getStatus());
            if (item.getIntent() != null) record.setIntent(item.getIntent());
            if (item.getAuthoredOn() != null) record.setAuthoredOn(item.getAuthoredOn());
            if (item.getDosageText() != null) record.setDosageText(item.getDosageText());
            if (item.getDoseQuantity() != null) record.setDoseQuantity(item.getDoseQuantity());
            if (item.getDoseUnit() != null) record.setDoseUnit(item.getDoseUnit());
            if (item.getRoute() != null) record.setRoute(item.getRoute());
            if (item.getFrequencyText() != null) record.setFrequencyText(item.getFrequencyText());
            if (item.getPrescriberName() != null) record.setPrescriberName(item.getPrescriberName());
            if (item.getReasonText() != null) record.setReasonText(item.getReasonText());
            if (item.getValidityStart() != null) record.setValidityStart(item.getValidityStart());
            if (item.getValidityEnd() != null) record.setValidityEnd(item.getValidityEnd());
            if (item.getRefillsAllowed() != null) record.setRefillsAllowed(item.getRefillsAllowed());
            if (item.getDisplayText() != null) record.setDisplayText(item.getDisplayText());
            record.setUpdatedAt(LocalDateTime.now());

            PatientMedicationDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null; // unreachable
        }
    }

    public boolean delete(@NonNull String extid) {

        PatientMedicationDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            record.setDeletedAt(LocalDateTime.now());
            record.setActive(ActiveEnum.INACTIVE);

            repository.save(record);
            log.info(getDeletedMessage(extid));
            return true;

        } catch (Exception e) {
            handleException("delete", extid, e);
            return false; // unreachable
        }
    }

    public PatientMedication findByExtid(@NonNull String extid) {
        PatientMedicationDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    /** Lookup by Epic's own resource id - returns null when not yet ingested. */
    public PatientMedication findByFhirResourceId(@NonNull String fhirResourceId) {
        return repository.findByFhirResourceId(fhirResourceId)
                .map(mapper::toModel)
                .orElse(null);
    }

    public List<PatientMedication> findByStatus(@NonNull String status) {
        return findAndLog(repository.findByStatus(status), String.format("status (%s)", status));
    }

    public List<PatientMedication> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<PatientMedication> findAll(Pageable pageable) {
        Page<PatientMedicationDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<PatientMedication> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<PatientMedication> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<PatientMedicationDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    private List<PatientMedication> findAndLog(List<PatientMedicationDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
