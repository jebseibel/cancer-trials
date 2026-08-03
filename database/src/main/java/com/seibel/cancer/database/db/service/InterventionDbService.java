package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.Intervention;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.InterventionDb;
import com.seibel.cancer.database.db.mapper.InterventionMapper;
import com.seibel.cancer.database.db.repository.InterventionRepository;
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
public class InterventionDbService extends BaseDbService {

    private final InterventionRepository repository;
    private final InterventionMapper mapper;

    public InterventionDbService(InterventionRepository repository, InterventionMapper mapper) {
        super("InterventionDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public Intervention create(@NonNull Intervention item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            InterventionDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            InterventionDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null;
        }
    }

    public Intervention create(@NonNull Long trialId, String type, @NonNull String name, String description) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            InterventionDb record = new InterventionDb();
            record.setExtid(extid);
            record.setTrialId(trialId);
            record.setType(type);
            record.setName(name);
            record.setDescription(description);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            InterventionDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public Intervention update(@NonNull String extid, Long trialId, String type, String name, String description) {

        InterventionDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (trialId != null) record.setTrialId(trialId);
            if (type != null) record.setType(type);
            if (name != null) record.setName(name);
            if (description != null) record.setDescription(description);
            record.setUpdatedAt(LocalDateTime.now());

            InterventionDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null;
        }
    }

    public boolean delete(@NonNull String extid) {

        InterventionDb record = repository.findByExtid(extid)
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

    public Intervention findByExtid(@NonNull String extid) {
        InterventionDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<Intervention> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public List<Intervention> findByTrialId(@NonNull Long trialId) {
        return findAndLog(repository.findByTrialIdAndActive(trialId, ActiveEnum.ACTIVE), String.format("trialId (%d)", trialId));
    }

    public Page<Intervention> findAll(Pageable pageable) {
        Page<InterventionDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<Intervention> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<Intervention> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<InterventionDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    private List<Intervention> findAndLog(List<InterventionDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
