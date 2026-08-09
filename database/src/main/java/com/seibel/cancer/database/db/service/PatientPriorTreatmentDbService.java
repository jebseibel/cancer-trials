package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.PatientPriorTreatment;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.entity.PatientPriorTreatmentDb;
import com.seibel.cancer.database.db.mapper.PatientPriorTreatmentMapper;
import com.seibel.cancer.database.db.repository.PatientPriorTreatmentRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * No positional-field create/update overload: PatientPriorTreatment has 24 business fields,
 * thirteen of them adjacent Strings holding the same five-state vocabulary. A positional call
 * would be trivially easy to transpose - exactly the case database-restapi-template says to
 * skip it for.
 */
@Slf4j
@Service
public class PatientPriorTreatmentDbService extends BaseDbService {

    private final PatientPriorTreatmentRepository repository;
    private final PatientPriorTreatmentMapper mapper;

    public PatientPriorTreatmentDbService(PatientPriorTreatmentRepository repository,
                                          PatientPriorTreatmentMapper mapper) {
        super("PatientPriorTreatmentDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public PatientPriorTreatment create(@NonNull PatientPriorTreatment item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            PatientPriorTreatmentDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            PatientPriorTreatmentDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public PatientPriorTreatment update(@NonNull String extid, @NonNull PatientPriorTreatment item) {

        PatientPriorTreatmentDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (item.getAppUserId() != null) record.setAppUserId(item.getAppUserId());
            if (item.getPatientDiagnosisId() != null) record.setPatientDiagnosisId(item.getPatientDiagnosisId());
            if (item.getCdk46Status() != null) record.setCdk46Status(item.getCdk46Status());
            if (item.getEndocrineStatus() != null) record.setEndocrineStatus(item.getEndocrineStatus());
            if (item.getSerdStatus() != null) record.setSerdStatus(item.getSerdStatus());
            if (item.getChemoStatus() != null) record.setChemoStatus(item.getChemoStatus());
            if (item.getHer2TherapyStatus() != null) record.setHer2TherapyStatus(item.getHer2TherapyStatus());
            if (item.getHer2AdcStatus() != null) record.setHer2AdcStatus(item.getHer2AdcStatus());
            if (item.getTrop2AdcStatus() != null) record.setTrop2AdcStatus(item.getTrop2AdcStatus());
            if (item.getParpStatus() != null) record.setParpStatus(item.getParpStatus());
            if (item.getPi3kAktMtorStatus() != null) record.setPi3kAktMtorStatus(item.getPi3kAktMtorStatus());
            if (item.getImmunotherapyStatus() != null) record.setImmunotherapyStatus(item.getImmunotherapyStatus());
            if (item.getTaxaneStatus() != null) record.setTaxaneStatus(item.getTaxaneStatus());
            if (item.getAnthracyclineStatus() != null) record.setAnthracyclineStatus(item.getAnthracyclineStatus());
            if (item.getPlatinumStatus() != null) record.setPlatinumStatus(item.getPlatinumStatus());
            if (item.getCurrentDrugNames() != null) record.setCurrentDrugNames(item.getCurrentDrugNames());
            if (item.getPriorDrugNames() != null) record.setPriorDrugNames(item.getPriorDrugNames());
            if (item.getLinesOfTherapyMetastatic() != null)
                record.setLinesOfTherapyMetastatic(item.getLinesOfTherapyMetastatic());
            if (item.getHadNeoadjuvant() != null) record.setHadNeoadjuvant(item.getHadNeoadjuvant());
            if (item.getHadAdjuvant() != null) record.setHadAdjuvant(item.getHadAdjuvant());
            if (item.getHadRadiation() != null) record.setHadRadiation(item.getHadRadiation());
            if (item.getHadSurgery() != null) record.setHadSurgery(item.getHadSurgery());
            if (item.getLastTreatmentEndDate() != null) record.setLastTreatmentEndDate(item.getLastTreatmentEndDate());
            if (item.getCurrentlyOnTreatment() != null) record.setCurrentlyOnTreatment(item.getCurrentlyOnTreatment());
            if (item.getOtherTreatments() != null) record.setOtherTreatments(item.getOtherTreatments());
            if (item.getNotes() != null) record.setNotes(item.getNotes());
            record.setUpdatedAt(LocalDateTime.now());

            PatientPriorTreatmentDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null; // unreachable
        }
    }

    public boolean delete(@NonNull String extid) {

        PatientPriorTreatmentDb record = repository.findByExtid(extid)
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

    public PatientPriorTreatment findByExtid(@NonNull String extid) {

        PatientPriorTreatmentDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<PatientPriorTreatment> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<PatientPriorTreatment> findAll(Pageable pageable) {
        return repository.findByActive(ActiveEnum.ACTIVE, pageable).map(mapper::toModel);
    }

    public List<PatientPriorTreatment> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum), String.format("active (%s)", activeEnum));
    }

    public Page<PatientPriorTreatment> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        return repository.findByActive(activeEnum, pageable).map(mapper::toModel);
    }

    public List<PatientPriorTreatment> findByAppUserId(@NonNull Long appUserId) {
        return findAndLog(repository.findByAppUserId(appUserId),
                String.format("appUserId (%d)", appUserId));
    }

    public List<PatientPriorTreatment> findByPatientDiagnosisId(@NonNull Long patientDiagnosisId) {
        return findAndLog(repository.findByPatientDiagnosisId(patientDiagnosisId),
                String.format("patientDiagnosisId (%d)", patientDiagnosisId));
    }

    private List<PatientPriorTreatment> findAndLog(List<PatientPriorTreatmentDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }
}
