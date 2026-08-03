package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.TrialDb;
import com.seibel.cancer.database.db.mapper.TrialMapper;
import com.seibel.cancer.database.db.repository.TrialRepository;
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
public class TrialDbService extends BaseDbService {

    private final TrialRepository repository;
    private final TrialMapper mapper;

    public TrialDbService(TrialRepository repository, TrialMapper mapper) {
        super("TrialDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public Trial create(@NonNull Trial item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            TrialDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            TrialDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public Trial update(@NonNull String extid, Trial item) {

        TrialDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (item.getNctId() != null) record.setNctId(item.getNctId());
            if (item.getBriefTitle() != null) record.setBriefTitle(item.getBriefTitle());
            if (item.getOfficialTitle() != null) record.setOfficialTitle(item.getOfficialTitle());
            if (item.getOverallStatus() != null) record.setOverallStatus(item.getOverallStatus());
            if (item.getStudyType() != null) record.setStudyType(item.getStudyType());
            if (item.getBriefSummary() != null) record.setBriefSummary(item.getBriefSummary());
            if (item.getDetailedDescription() != null) record.setDetailedDescription(item.getDetailedDescription());
            if (item.getStartDate() != null) record.setStartDate(item.getStartDate());
            if (item.getPrimaryCompletionDate() != null) record.setPrimaryCompletionDate(item.getPrimaryCompletionDate());
            if (item.getCompletionDate() != null) record.setCompletionDate(item.getCompletionDate());
            if (item.getLastUpdatePostedDate() != null) record.setLastUpdatePostedDate(item.getLastUpdatePostedDate());
            if (item.getEnrollmentCount() != null) record.setEnrollmentCount(item.getEnrollmentCount());
            if (item.getEnrollmentType() != null) record.setEnrollmentType(item.getEnrollmentType());
            if (item.getHealthyVolunteers() != null) record.setHealthyVolunteers(item.getHealthyVolunteers());
            if (item.getSex() != null) record.setSex(item.getSex());
            if (item.getMinimumAge() != null) record.setMinimumAge(item.getMinimumAge());
            if (item.getMaximumAge() != null) record.setMaximumAge(item.getMaximumAge());
            if (item.getEligibilityCriteria() != null) record.setEligibilityCriteria(item.getEligibilityCriteria());
            if (item.getIsPaidStudy() != null) record.setIsPaidStudy(item.getIsPaidStudy());
            if (item.getPaidAmount() != null) record.setPaidAmount(item.getPaidAmount());
            if (item.getPrimaryTrialSourceId() != null) record.setPrimaryTrialSourceId(item.getPrimaryTrialSourceId());
            record.setUpdatedAt(LocalDateTime.now());

            TrialDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null; // unreachable
        }
    }

    public boolean delete(@NonNull String extid) {

        TrialDb record = repository.findByExtid(extid)
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

    public Trial findByExtid(@NonNull String extid) {
        TrialDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public Trial findByNctId(@NonNull String nctId) {
        return repository.findByNctId(nctId)
                .map(record -> {
                    log.info("findByNctId(): found nctId={}", nctId);
                    return mapper.toModel(record);
                })
                .orElse(null);
    }

    public List<Trial> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<Trial> findAll(Pageable pageable) {
        Page<TrialDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<Trial> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<Trial> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<TrialDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    private List<Trial> findAndLog(List<TrialDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
