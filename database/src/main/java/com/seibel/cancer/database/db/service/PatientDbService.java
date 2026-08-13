package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.Patient;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.entity.PatientDb;
import com.seibel.cancer.database.db.mapper.PatientMapper;
import com.seibel.cancer.database.db.repository.PatientRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class PatientDbService extends BaseDbService {

    private final PatientRepository repository;
    private final PatientMapper mapper;

    public PatientDbService(PatientRepository repository, PatientMapper mapper) {
        super("PatientDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public Patient create(@NonNull Patient item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            PatientDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            PatientDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public Patient create(@NonNull String displayName, String fullName, LocalDate dateOfBirth,
                          String sex, String notes) {
        return create(Patient.builder()
                .displayName(displayName)
                .fullName(fullName)
                .dateOfBirth(dateOfBirth)
                .sex(sex)
                .notes(notes)
                .build());
    }

    public Patient update(@NonNull String extid, @NonNull Patient item) {

        PatientDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (item.getDisplayName() != null) record.setDisplayName(item.getDisplayName());
            if (item.getFullName() != null) record.setFullName(item.getFullName());
            if (item.getDateOfBirth() != null) record.setDateOfBirth(item.getDateOfBirth());
            if (item.getSex() != null) record.setSex(item.getSex());
            if (item.getNotes() != null) record.setNotes(item.getNotes());
            record.setUpdatedAt(LocalDateTime.now());

            PatientDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null; // unreachable
        }
    }

    public Patient update(@NonNull String extid, String displayName, String fullName,
                          LocalDate dateOfBirth, String sex, String notes) {
        return update(extid, Patient.builder()
                .displayName(displayName)
                .fullName(fullName)
                .dateOfBirth(dateOfBirth)
                .sex(sex)
                .notes(notes)
                .build());
    }

    public boolean delete(@NonNull String extid) {

        PatientDb record = repository.findByExtid(extid)
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

    public Patient findByExtid(@NonNull String extid) {

        PatientDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<Patient> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<Patient> findAll(Pageable pageable) {
        return repository.findByActive(ActiveEnum.ACTIVE, pageable).map(mapper::toModel);
    }

    public List<Patient> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum), String.format("active (%s)", activeEnum));
    }

    public Page<Patient> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        return repository.findByActive(activeEnum, pageable).map(mapper::toModel);
    }

    private List<Patient> findAndLog(List<PatientDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }
}
