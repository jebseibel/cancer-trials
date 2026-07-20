package com.seibel.jobhunting.database.db.service;

import com.seibel.jobhunting.common.domain.Application;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.common.enums.ApplicationStatus;
import com.seibel.jobhunting.common.exceptions.ServiceException;
import com.seibel.jobhunting.database.db.entity.ApplicationDb;
import com.seibel.jobhunting.database.db.mapper.ApplicationMapper;
import com.seibel.jobhunting.database.db.repository.ApplicationRepository;
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
public class ApplicationDbService extends BaseDbService {

    private final ApplicationRepository repository;
    private final ApplicationMapper mapper;

    public ApplicationDbService(ApplicationRepository repository, ApplicationMapper mapper) {
        super("ApplicationDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public Application create(@NonNull Application item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            ApplicationDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            ApplicationDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null;
        }
    }

    public Application create(@NonNull Long jobPostingId, @NonNull LocalDate dateApplied, String resumeVersion,
                               @NonNull ApplicationStatus applicationStatus, String notes) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            ApplicationDb record = new ApplicationDb();
            record.setExtid(extid);
            record.setJobPostingId(jobPostingId);
            record.setDateApplied(dateApplied);
            record.setResumeVersion(resumeVersion);
            record.setApplicationStatus(applicationStatus);
            record.setNotes(notes);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            ApplicationDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public Application update(@NonNull String extid, Long jobPostingId, LocalDate dateApplied, String resumeVersion,
                               ApplicationStatus applicationStatus, String notes) {

        ApplicationDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (jobPostingId != null) record.setJobPostingId(jobPostingId);
            if (dateApplied != null) record.setDateApplied(dateApplied);
            if (resumeVersion != null) record.setResumeVersion(resumeVersion);
            if (applicationStatus != null) record.setApplicationStatus(applicationStatus);
            if (notes != null) record.setNotes(notes);
            record.setUpdatedAt(LocalDateTime.now());

            ApplicationDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null;
        }
    }

    public boolean delete(@NonNull String extid) {

        ApplicationDb record = repository.findByExtid(extid)
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

    public Application findByExtid(@NonNull String extid) {
        ApplicationDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<Application> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<Application> findAll(Pageable pageable) {
        Page<ApplicationDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<Application> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<Application> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<ApplicationDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<Application> findByJobPostingId(@NonNull Long jobPostingId) {
        return findAndLog(repository.findByJobPostingId(jobPostingId), String.format("jobPostingId (%s)", jobPostingId));
    }

    private List<Application> findAndLog(List<ApplicationDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
