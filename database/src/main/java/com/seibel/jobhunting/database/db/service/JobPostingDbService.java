package com.seibel.jobhunting.database.db.service;

import com.seibel.jobhunting.common.domain.JobPosting;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.common.enums.JobPostingStatus;
import com.seibel.jobhunting.common.enums.JobSource;
import com.seibel.jobhunting.common.enums.WorkMode;
import com.seibel.jobhunting.common.exceptions.ServiceException;
import com.seibel.jobhunting.database.db.entity.JobPostingDb;
import com.seibel.jobhunting.database.db.mapper.JobPostingMapper;
import com.seibel.jobhunting.database.db.repository.JobPostingRepository;
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
public class JobPostingDbService extends BaseDbService {

    private final JobPostingRepository repository;
    private final JobPostingMapper mapper;

    public JobPostingDbService(JobPostingRepository repository, JobPostingMapper mapper) {
        super("JobPostingDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public JobPosting create(@NonNull JobPosting item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            JobPostingDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            JobPostingDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null;
        }
    }

    public JobPosting create(@NonNull String title, @NonNull Long companyId, String description, String city, String state, String country,
                              WorkMode workMode, Integer salaryMin, Integer salaryMax, String salaryCurrency,
                              @NonNull JobSource source, @NonNull String sourceUrl, LocalDateTime postedAt,
                              @NonNull JobPostingStatus status, String notes) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            JobPostingDb record = new JobPostingDb();
            record.setExtid(extid);
            record.setTitle(title);
            record.setCompanyId(companyId);
            record.setDescription(description);
            record.setCity(city);
            record.setState(state);
            record.setCountry(country);
            record.setWorkMode(workMode);
            record.setSalaryMin(salaryMin);
            record.setSalaryMax(salaryMax);
            record.setSalaryCurrency(salaryCurrency);
            record.setSource(source);
            record.setSourceUrl(sourceUrl);
            record.setPostedAt(postedAt);
            record.setStatus(status);
            record.setNotes(notes);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            JobPostingDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public JobPosting update(@NonNull String extid, String title, Long companyId, String description, String city, String state, String country,
                              WorkMode workMode, Integer salaryMin, Integer salaryMax, String salaryCurrency,
                              JobSource source, String sourceUrl, LocalDateTime postedAt,
                              JobPostingStatus status, String notes) {

        JobPostingDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (title != null) record.setTitle(title);
            if (companyId != null) record.setCompanyId(companyId);
            if (description != null) record.setDescription(description);
            if (city != null) record.setCity(city);
            if (state != null) record.setState(state);
            if (country != null) record.setCountry(country);
            if (workMode != null) record.setWorkMode(workMode);
            if (salaryMin != null) record.setSalaryMin(salaryMin);
            if (salaryMax != null) record.setSalaryMax(salaryMax);
            if (salaryCurrency != null) record.setSalaryCurrency(salaryCurrency);
            if (source != null) record.setSource(source);
            if (sourceUrl != null) record.setSourceUrl(sourceUrl);
            if (postedAt != null) record.setPostedAt(postedAt);
            if (status != null) record.setStatus(status);
            if (notes != null) record.setNotes(notes);
            record.setUpdatedAt(LocalDateTime.now());

            JobPostingDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null;
        }
    }

    public boolean delete(@NonNull String extid) {

        JobPostingDb record = repository.findByExtid(extid)
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

    public JobPosting findByExtid(@NonNull String extid) {
        JobPostingDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<JobPosting> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<JobPosting> findAll(Pageable pageable) {
        Page<JobPostingDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<JobPosting> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<JobPosting> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<JobPostingDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    private List<JobPosting> findAndLog(List<JobPostingDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
