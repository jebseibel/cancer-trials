package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.SavedTrialMatch;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.entity.SavedTrialMatchDb;
import com.seibel.cancer.database.db.mapper.SavedTrialMatchMapper;
import com.seibel.cancer.database.db.repository.SavedTrialMatchRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * No positional-field create/update overload: SavedTrialMatch has 13 business fields, five of
 * them adjacent Strings. A positional call would be trivially easy to transpose, which is
 * exactly the case database-restapi-template says to skip it for.
 */
@Slf4j
@Service
public class SavedTrialMatchDbService extends BaseDbService {

    private final SavedTrialMatchRepository repository;
    private final SavedTrialMatchMapper mapper;

    public SavedTrialMatchDbService(SavedTrialMatchRepository repository, SavedTrialMatchMapper mapper) {
        super("SavedTrialMatchDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public SavedTrialMatch create(@NonNull SavedTrialMatch item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            SavedTrialMatchDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            SavedTrialMatchDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public SavedTrialMatch update(@NonNull String extid, @NonNull SavedTrialMatch item) {

        SavedTrialMatchDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (item.getTrialId() != null) record.setTrialId(item.getTrialId());
            if (item.getPatientId() != null) record.setPatientId(item.getPatientId());
            if (item.getPatientDiagnosisId() != null) record.setPatientDiagnosisId(item.getPatientDiagnosisId());
            if (item.getSearchRunId() != null) record.setSearchRunId(item.getSearchRunId());
            if (item.getQueryText() != null) record.setQueryText(item.getQueryText());
            if (item.getTopScore() != null) record.setTopScore(item.getTopScore());
            if (item.getMatchRank() != null) record.setMatchRank(item.getMatchRank());
            if (item.getSnapshotErStatus() != null) record.setSnapshotErStatus(item.getSnapshotErStatus());
            if (item.getSnapshotPrStatus() != null) record.setSnapshotPrStatus(item.getSnapshotPrStatus());
            if (item.getSnapshotHer2Status() != null) record.setSnapshotHer2Status(item.getSnapshotHer2Status());
            if (item.getSnapshotStage() != null) record.setSnapshotStage(item.getSnapshotStage());
            if (item.getSnapshotBiomarkers() != null) record.setSnapshotBiomarkers(item.getSnapshotBiomarkers());
            if (item.getMatchedAt() != null) record.setMatchedAt(item.getMatchedAt());
            record.setUpdatedAt(LocalDateTime.now());

            SavedTrialMatchDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null; // unreachable
        }
    }

    public boolean delete(@NonNull String extid) {

        SavedTrialMatchDb record = repository.findByExtid(extid)
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

    public SavedTrialMatch findByExtid(@NonNull String extid) {

        SavedTrialMatchDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<SavedTrialMatch> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<SavedTrialMatch> findAll(Pageable pageable) {
        return repository.findByActive(ActiveEnum.ACTIVE, pageable).map(mapper::toModel);
    }

    public List<SavedTrialMatch> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum), String.format("active (%s)", activeEnum));
    }

    public Page<SavedTrialMatch> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        return repository.findByActive(activeEnum, pageable).map(mapper::toModel);
    }

    public List<SavedTrialMatch> findBySearchRunId(@NonNull String searchRunId) {
        return findAndLog(repository.findBySearchRunId(searchRunId),
                String.format("searchRunId (%s)", searchRunId));
    }

    public List<SavedTrialMatch> findByPatientId(@NonNull Long patientId) {
        return findAndLog(repository.findByPatientId(patientId),
                String.format("patientId (%d)", patientId));
    }

    public List<SavedTrialMatch> findByTrialId(@NonNull Long trialId) {
        return findAndLog(repository.findByTrialId(trialId),
                String.format("trialId (%d)", trialId));
    }

    private List<SavedTrialMatch> findAndLog(List<SavedTrialMatchDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }
}
