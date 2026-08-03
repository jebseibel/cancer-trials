package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.Outcome;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.OutcomeDb;
import com.seibel.cancer.database.db.mapper.OutcomeMapper;
import com.seibel.cancer.database.db.repository.OutcomeRepository;
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
public class OutcomeDbService extends BaseDbService {

    private final OutcomeRepository repository;
    private final OutcomeMapper mapper;

    public OutcomeDbService(OutcomeRepository repository, OutcomeMapper mapper) {
        super("OutcomeDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public Outcome create(@NonNull Outcome item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            OutcomeDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            OutcomeDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null;
        }
    }

    public Outcome create(@NonNull Long trialId, @NonNull String outcomeType, @NonNull String measure,
                           String description, String timeFrame) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            OutcomeDb record = new OutcomeDb();
            record.setExtid(extid);
            record.setTrialId(trialId);
            record.setOutcomeType(outcomeType);
            record.setMeasure(measure);
            record.setDescription(description);
            record.setTimeFrame(timeFrame);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            OutcomeDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public Outcome update(@NonNull String extid, Long trialId, String outcomeType, String measure,
                           String description, String timeFrame) {

        OutcomeDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (trialId != null) record.setTrialId(trialId);
            if (outcomeType != null) record.setOutcomeType(outcomeType);
            if (measure != null) record.setMeasure(measure);
            if (description != null) record.setDescription(description);
            if (timeFrame != null) record.setTimeFrame(timeFrame);
            record.setUpdatedAt(LocalDateTime.now());

            OutcomeDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null;
        }
    }

    public boolean delete(@NonNull String extid) {

        OutcomeDb record = repository.findByExtid(extid)
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

    public Outcome findByExtid(@NonNull String extid) {
        OutcomeDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<Outcome> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public List<Outcome> findByTrialId(@NonNull Long trialId) {
        return findAndLog(repository.findByTrialIdAndActive(trialId, ActiveEnum.ACTIVE), String.format("trialId (%d)", trialId));
    }

    public Page<Outcome> findAll(Pageable pageable) {
        Page<OutcomeDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<Outcome> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<Outcome> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<OutcomeDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    private List<Outcome> findAndLog(List<OutcomeDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
