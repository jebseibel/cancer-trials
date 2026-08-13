package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.PatientDiagnosis;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.entity.PatientDiagnosisDb;
import com.seibel.cancer.database.db.mapper.PatientDiagnosisMapper;
import com.seibel.cancer.database.db.repository.PatientDiagnosisRepository;
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
public class PatientDiagnosisDbService extends BaseDbService {

    private final PatientDiagnosisRepository repository;
    private final PatientDiagnosisMapper mapper;

    public PatientDiagnosisDbService(PatientDiagnosisRepository repository, PatientDiagnosisMapper mapper) {
        super("PatientDiagnosisDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    // No positional-field create/update overload: 21 fields is far past the point where a
    // positional call is safe to read or write. Domain-object only, same as LabResult.

    public PatientDiagnosis create(@NonNull PatientDiagnosis item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            PatientDiagnosisDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            PatientDiagnosisDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public PatientDiagnosis update(@NonNull String extid, @NonNull PatientDiagnosis item) {

        PatientDiagnosisDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (item.getPatientId() != null) record.setPatientId(item.getPatientId());
            if (item.getCancerType() != null) record.setCancerType(item.getCancerType());
            if (item.getStage() != null) record.setStage(item.getStage());
            if (item.getStageSystem() != null) record.setStageSystem(item.getStageSystem());
            if (item.getIsMetastatic() != null) record.setIsMetastatic(item.getIsMetastatic());
            if (item.getMetastasisSites() != null) record.setMetastasisSites(item.getMetastasisSites());
            if (item.getReceptorSubtype() != null) record.setReceptorSubtype(item.getReceptorSubtype());
            if (item.getErStatus() != null) record.setErStatus(item.getErStatus());
            if (item.getPrStatus() != null) record.setPrStatus(item.getPrStatus());
            if (item.getHer2Status() != null) record.setHer2Status(item.getHer2Status());
            if (item.getBiomarkers() != null) record.setBiomarkers(item.getBiomarkers());
            if (item.getEcogStatus() != null) record.setEcogStatus(item.getEcogStatus());
            if (item.getPriorChemoRegimens() != null) record.setPriorChemoRegimens(item.getPriorChemoRegimens());
            if (item.getLastChemoEndDate() != null) record.setLastChemoEndDate(item.getLastChemoEndDate());
            if (item.getPriorTreatments() != null) record.setPriorTreatments(item.getPriorTreatments());
            if (item.getHasMeasurableDisease() != null) record.setHasMeasurableDisease(item.getHasMeasurableDisease());
            if (item.getMenopausalStatus() != null) record.setMenopausalStatus(item.getMenopausalStatus());
            if (item.getDiagnosisDate() != null) record.setDiagnosisDate(item.getDiagnosisDate());
            if (item.getNotes() != null) record.setNotes(item.getNotes());
            record.setUpdatedAt(LocalDateTime.now());

            PatientDiagnosisDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null; // unreachable
        }
    }

    public boolean delete(@NonNull String extid) {

        PatientDiagnosisDb record = repository.findByExtid(extid)
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

    public PatientDiagnosis findByExtid(@NonNull String extid) {
        PatientDiagnosisDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    /** One patient's diagnosis, looked up by the owning app user. */
    public List<PatientDiagnosis> findByPatientId(@NonNull Long patientId) {
        return findAndLog(repository.findByPatientId(patientId),
                String.format("patientId (%d)", patientId));
    }

    public List<PatientDiagnosis> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<PatientDiagnosis> findAll(Pageable pageable) {
        Page<PatientDiagnosisDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<PatientDiagnosis> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<PatientDiagnosis> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<PatientDiagnosisDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    private List<PatientDiagnosis> findAndLog(List<PatientDiagnosisDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
