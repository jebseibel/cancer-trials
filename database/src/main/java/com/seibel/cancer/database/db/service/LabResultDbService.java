package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.LabResult;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.entity.LabResultDb;
import com.seibel.cancer.database.db.mapper.LabResultMapper;
import com.seibel.cancer.database.db.repository.LabResultRepository;
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
public class LabResultDbService extends BaseDbService {

    private final LabResultRepository repository;
    private final LabResultMapper mapper;

    public LabResultDbService(LabResultRepository repository, LabResultMapper mapper) {
        super("LabResultDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    // No positional-field create/update overload: 16 fields is far past the point where a
    // positional call is safe to read or write. Domain-object only, same as PatientMedication.

    public LabResult create(@NonNull LabResult item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            LabResultDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            LabResultDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public LabResult update(@NonNull String extid, @NonNull LabResult item) {

        LabResultDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (item.getFhirResourceId() != null) record.setFhirResourceId(item.getFhirResourceId());
            if (item.getTestName() != null) record.setTestName(item.getTestName());
            if (item.getLoincCode() != null) record.setLoincCode(item.getLoincCode());
            if (item.getStatus() != null) record.setStatus(item.getStatus());
            if (item.getCategory() != null) record.setCategory(item.getCategory());
            if (item.getEffectiveAt() != null) record.setEffectiveAt(item.getEffectiveAt());
            if (item.getIssuedAt() != null) record.setIssuedAt(item.getIssuedAt());
            if (item.getValueQuantity() != null) record.setValueQuantity(item.getValueQuantity());
            if (item.getValueUnit() != null) record.setValueUnit(item.getValueUnit());
            if (item.getValueString() != null) record.setValueString(item.getValueString());
            if (item.getInterpretation() != null) record.setInterpretation(item.getInterpretation());
            if (item.getReferenceRangeLow() != null) record.setReferenceRangeLow(item.getReferenceRangeLow());
            if (item.getReferenceRangeHigh() != null) record.setReferenceRangeHigh(item.getReferenceRangeHigh());
            if (item.getReferenceRangeText() != null) record.setReferenceRangeText(item.getReferenceRangeText());
            if (item.getIsPanel() != null) record.setIsPanel(item.getIsPanel());
            if (item.getDisplayText() != null) record.setDisplayText(item.getDisplayText());
            record.setUpdatedAt(LocalDateTime.now());

            LabResultDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null; // unreachable
        }
    }

    public boolean delete(@NonNull String extid) {

        LabResultDb record = repository.findByExtid(extid)
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

    public LabResult findByExtid(@NonNull String extid) {
        LabResultDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    /** Lookup by Epic's own resource id - returns null when not yet ingested. */
    public LabResult findByFhirResourceId(@NonNull String fhirResourceId) {
        return repository.findByFhirResourceId(fhirResourceId)
                .map(mapper::toModel)
                .orElse(null);
    }

    public List<LabResult> findByLoincCode(@NonNull String loincCode) {
        return findAndLog(repository.findByLoincCodeOrderByEffectiveAtDesc(loincCode),
                String.format("loincCode (%s)", loincCode));
    }

    public List<LabResult> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<LabResult> findAll(Pageable pageable) {
        Page<LabResultDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<LabResult> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<LabResult> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<LabResultDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    private List<LabResult> findAndLog(List<LabResultDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
