package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.PatientVariant;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.entity.PatientVariantDb;
import com.seibel.cancer.database.db.mapper.PatientVariantMapper;
import com.seibel.cancer.database.db.repository.PatientVariantRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * No positional-field create/update overload: PatientVariant has 21 business fields, sixteen of
 * them adjacent Strings holding the same five-state vocabulary. A positional call would be
 * almost impossible to get right and trivially easy to transpose - exactly the case
 * database-restapi-template says to skip it for.
 */
@Slf4j
@Service
public class PatientVariantDbService extends BaseDbService {

    private final PatientVariantRepository repository;
    private final PatientVariantMapper mapper;

    public PatientVariantDbService(PatientVariantRepository repository, PatientVariantMapper mapper) {
        super("PatientVariantDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public PatientVariant create(@NonNull PatientVariant item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            PatientVariantDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            PatientVariantDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public PatientVariant update(@NonNull String extid, @NonNull PatientVariant item) {

        PatientVariantDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (item.getAppUserId() != null) record.setAppUserId(item.getAppUserId());
            if (item.getPatientDiagnosisId() != null) record.setPatientDiagnosisId(item.getPatientDiagnosisId());
            if (item.getPik3caStatus() != null) record.setPik3caStatus(item.getPik3caStatus());
            if (item.getEsr1Status() != null) record.setEsr1Status(item.getEsr1Status());
            if (item.getTp53Status() != null) record.setTp53Status(item.getTp53Status());
            if (item.getAkt1Status() != null) record.setAkt1Status(item.getAkt1Status());
            if (item.getPtenStatus() != null) record.setPtenStatus(item.getPtenStatus());
            if (item.getErbb2SomaticStatus() != null) record.setErbb2SomaticStatus(item.getErbb2SomaticStatus());
            if (item.getBrca1Status() != null) record.setBrca1Status(item.getBrca1Status());
            if (item.getBrca2Status() != null) record.setBrca2Status(item.getBrca2Status());
            if (item.getPalb2Status() != null) record.setPalb2Status(item.getPalb2Status());
            if (item.getAtmStatus() != null) record.setAtmStatus(item.getAtmStatus());
            if (item.getChek2Status() != null) record.setChek2Status(item.getChek2Status());
            if (item.getHrdStatus() != null) record.setHrdStatus(item.getHrdStatus());
            if (item.getPdl1Status() != null) record.setPdl1Status(item.getPdl1Status());
            if (item.getKi67Percent() != null) record.setKi67Percent(item.getKi67Percent());
            if (item.getGermlineTestDone() != null) record.setGermlineTestDone(item.getGermlineTestDone());
            if (item.getSomaticTestDone() != null) record.setSomaticTestDone(item.getSomaticTestDone());
            if (item.getTestDate() != null) record.setTestDate(item.getTestDate());
            if (item.getTestLab() != null) record.setTestLab(item.getTestLab());
            if (item.getOtherVariants() != null) record.setOtherVariants(item.getOtherVariants());
            if (item.getNotes() != null) record.setNotes(item.getNotes());
            record.setUpdatedAt(LocalDateTime.now());

            PatientVariantDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null; // unreachable
        }
    }

    public boolean delete(@NonNull String extid) {

        PatientVariantDb record = repository.findByExtid(extid)
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

    public PatientVariant findByExtid(@NonNull String extid) {

        PatientVariantDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<PatientVariant> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<PatientVariant> findAll(Pageable pageable) {
        return repository.findByActive(ActiveEnum.ACTIVE, pageable).map(mapper::toModel);
    }

    public List<PatientVariant> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum), String.format("active (%s)", activeEnum));
    }

    public Page<PatientVariant> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        return repository.findByActive(activeEnum, pageable).map(mapper::toModel);
    }

    public List<PatientVariant> findByAppUserId(@NonNull Long appUserId) {
        return findAndLog(repository.findByAppUserId(appUserId),
                String.format("appUserId (%d)", appUserId));
    }

    public List<PatientVariant> findByPatientDiagnosisId(@NonNull Long patientDiagnosisId) {
        return findAndLog(repository.findByPatientDiagnosisId(patientDiagnosisId),
                String.format("patientDiagnosisId (%d)", patientDiagnosisId));
    }

    private List<PatientVariant> findAndLog(List<PatientVariantDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }
}
