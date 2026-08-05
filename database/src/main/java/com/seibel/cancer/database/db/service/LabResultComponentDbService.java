package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.LabResultComponent;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.entity.LabResultComponentDb;
import com.seibel.cancer.database.db.mapper.LabResultComponentMapper;
import com.seibel.cancer.database.db.repository.LabResultComponentRepository;
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
public class LabResultComponentDbService extends BaseDbService {

    private final LabResultComponentRepository repository;
    private final LabResultComponentMapper mapper;

    public LabResultComponentDbService(LabResultComponentRepository repository, LabResultComponentMapper mapper) {
        super("LabResultComponentDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public LabResultComponent create(@NonNull LabResultComponent item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            LabResultComponentDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            LabResultComponentDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public LabResultComponent update(@NonNull String extid, @NonNull LabResultComponent item) {

        LabResultComponentDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (item.getLabResultId() != null) record.setLabResultId(item.getLabResultId());
            if (item.getComponentName() != null) record.setComponentName(item.getComponentName());
            if (item.getLoincCode() != null) record.setLoincCode(item.getLoincCode());
            if (item.getValueQuantity() != null) record.setValueQuantity(item.getValueQuantity());
            if (item.getValueUnit() != null) record.setValueUnit(item.getValueUnit());
            if (item.getValueString() != null) record.setValueString(item.getValueString());
            if (item.getInterpretation() != null) record.setInterpretation(item.getInterpretation());
            if (item.getReferenceRangeLow() != null) record.setReferenceRangeLow(item.getReferenceRangeLow());
            if (item.getReferenceRangeHigh() != null) record.setReferenceRangeHigh(item.getReferenceRangeHigh());
            if (item.getReferenceRangeText() != null) record.setReferenceRangeText(item.getReferenceRangeText());
            if (item.getDisplayText() != null) record.setDisplayText(item.getDisplayText());
            record.setUpdatedAt(LocalDateTime.now());

            LabResultComponentDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null; // unreachable
        }
    }

    public boolean delete(@NonNull String extid) {

        LabResultComponentDb record = repository.findByExtid(extid)
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

    public LabResultComponent findByExtid(@NonNull String extid) {
        LabResultComponentDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    /** All components of one panel. Also the lookup used when re-normalizing a parent. */
    public List<LabResultComponent> findByLabResultId(@NonNull Long labResultId) {
        return findAndLog(repository.findByLabResultId(labResultId),
                String.format("labResultId (%d)", labResultId));
    }

    public List<LabResultComponent> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<LabResultComponent> findAll(Pageable pageable) {
        Page<LabResultComponentDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<LabResultComponent> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<LabResultComponent> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<LabResultComponentDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    private List<LabResultComponent> findAndLog(List<LabResultComponentDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
