package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.TrialStatus;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.TrialStatusDb;
import com.seibel.cancer.database.db.mapper.TrialStatusMapper;
import com.seibel.cancer.database.db.repository.TrialStatusRepository;
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
public class TrialStatusDbService extends BaseDbService {

    private final TrialStatusRepository repository;
    private final TrialStatusMapper mapper;

    public TrialStatusDbService(TrialStatusRepository repository, TrialStatusMapper mapper) {
        super("TrialStatusDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public TrialStatus create(@NonNull TrialStatus item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            TrialStatusDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            TrialStatusDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null;
        }
    }

    public TrialStatus create(@NonNull Long trialId, @NonNull Long appUserId, @NonNull String status,
                               String notes, LocalDateTime statusChangedAt) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            TrialStatusDb record = new TrialStatusDb();
            record.setExtid(extid);
            record.setTrialId(trialId);
            record.setAppUserId(appUserId);
            record.setStatus(status);
            record.setNotes(notes);
            record.setStatusChangedAt(statusChangedAt);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            TrialStatusDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public TrialStatus update(@NonNull String extid, Long trialId, Long appUserId, String status,
                               String notes, LocalDateTime statusChangedAt) {

        TrialStatusDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (trialId != null) record.setTrialId(trialId);
            if (appUserId != null) record.setAppUserId(appUserId);
            if (status != null) record.setStatus(status);
            if (notes != null) record.setNotes(notes);
            if (statusChangedAt != null) record.setStatusChangedAt(statusChangedAt);
            record.setUpdatedAt(LocalDateTime.now());

            TrialStatusDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null;
        }
    }

    public boolean delete(@NonNull String extid) {

        TrialStatusDb record = repository.findByExtid(extid)
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

    public TrialStatus findByExtid(@NonNull String extid) {
        TrialStatusDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<TrialStatus> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public List<TrialStatus> findByAppUserId(@NonNull Long appUserId) {
        return findAndLog(repository.findByAppUserId(appUserId), String.format("appUserId (%d)", appUserId));
    }

    public Page<TrialStatus> findAll(Pageable pageable) {
        Page<TrialStatusDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<TrialStatus> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<TrialStatus> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<TrialStatusDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    private List<TrialStatus> findAndLog(List<TrialStatusDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
