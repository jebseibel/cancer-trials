package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.Medication;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.MedicationDb;
import com.seibel.cancer.database.db.mapper.MedicationMapper;
import com.seibel.cancer.database.db.repository.MedicationRepository;
import com.seibel.cancer.common.exceptions.ServiceException;
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
public class MedicationDbService extends BaseDbService {

    private final MedicationRepository repository;
    private final MedicationMapper mapper;

    public MedicationDbService(MedicationRepository repository, MedicationMapper mapper) {
        super("MedicationDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public Medication create(@NonNull Medication item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            MedicationDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            MedicationDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public Medication create(@NonNull String name) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            MedicationDb record = new MedicationDb();
            record.setExtid(extid);
            record.setName(name);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            MedicationDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public Medication update(@NonNull String extid, String name) {

        MedicationDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (name != null) record.setName(name);
            record.setUpdatedAt(LocalDateTime.now());

            MedicationDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null; // unreachable
        }
    }

    public boolean delete(@NonNull String extid) {

        MedicationDb record = repository.findByExtid(extid)
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

    public Medication findByExtid(@NonNull String extid) {
        MedicationDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<Medication> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<Medication> findAll(Pageable pageable) {
        Page<MedicationDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<Medication> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<Medication> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<MedicationDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    private List<Medication> findAndLog(List<MedicationDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
