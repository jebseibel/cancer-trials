package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.OverallOfficial;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.OverallOfficialDb;
import com.seibel.cancer.database.db.mapper.OverallOfficialMapper;
import com.seibel.cancer.database.db.repository.OverallOfficialRepository;
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
public class OverallOfficialDbService extends BaseDbService {

    private final OverallOfficialRepository repository;
    private final OverallOfficialMapper mapper;

    public OverallOfficialDbService(OverallOfficialRepository repository, OverallOfficialMapper mapper) {
        super("OverallOfficialDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public OverallOfficial create(@NonNull OverallOfficial item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            OverallOfficialDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            OverallOfficialDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null;
        }
    }

    public OverallOfficial create(@NonNull Long trialId, @NonNull String name, String affiliation, String role) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            OverallOfficialDb record = new OverallOfficialDb();
            record.setExtid(extid);
            record.setTrialId(trialId);
            record.setName(name);
            record.setAffiliation(affiliation);
            record.setRole(role);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            OverallOfficialDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public OverallOfficial update(@NonNull String extid, Long trialId, String name, String affiliation, String role) {

        OverallOfficialDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (trialId != null) record.setTrialId(trialId);
            if (name != null) record.setName(name);
            if (affiliation != null) record.setAffiliation(affiliation);
            if (role != null) record.setRole(role);
            record.setUpdatedAt(LocalDateTime.now());

            OverallOfficialDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null;
        }
    }

    public boolean delete(@NonNull String extid) {

        OverallOfficialDb record = repository.findByExtid(extid)
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

    public OverallOfficial findByExtid(@NonNull String extid) {
        OverallOfficialDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<OverallOfficial> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public List<OverallOfficial> findByTrialId(@NonNull Long trialId) {
        return findAndLog(repository.findByTrialIdAndActive(trialId, ActiveEnum.ACTIVE), String.format("trialId (%d)", trialId));
    }

    public Page<OverallOfficial> findAll(Pageable pageable) {
        Page<OverallOfficialDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<OverallOfficial> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<OverallOfficial> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<OverallOfficialDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    private List<OverallOfficial> findAndLog(List<OverallOfficialDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
