package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.StagingRawFhirResource;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.StagingRawFhirResourceDb;
import com.seibel.cancer.database.db.mapper.StagingRawFhirResourceMapper;
import com.seibel.cancer.database.db.repository.StagingRawFhirResourceRepository;
import com.seibel.cancer.common.exceptions.ServiceException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class StagingRawFhirResourceDbService extends BaseDbService {

    private final StagingRawFhirResourceRepository repository;
    private final StagingRawFhirResourceMapper mapper;

    public StagingRawFhirResourceDbService(StagingRawFhirResourceRepository repository, StagingRawFhirResourceMapper mapper) {
        super("StagingRawFhirResourceDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public StagingRawFhirResource create(@NonNull StagingRawFhirResource item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            StagingRawFhirResourceDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            StagingRawFhirResourceDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null;
        }
    }

    public StagingRawFhirResource create(@NonNull String resourceType, @NonNull String fhirResourceId, String rawPayload,
                                          @NonNull LocalDateTime fetchedAt, LocalDateTime normalizedAt, String normalizationError) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            StagingRawFhirResourceDb record = new StagingRawFhirResourceDb();
            record.setExtid(extid);
            record.setResourceType(resourceType);
            record.setFhirResourceId(fhirResourceId);
            record.setRawPayload(rawPayload);
            record.setFetchedAt(fetchedAt);
            record.setNormalizedAt(normalizedAt);
            record.setNormalizationError(normalizationError);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            StagingRawFhirResourceDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public StagingRawFhirResource update(@NonNull String extid, String resourceType, String fhirResourceId, String rawPayload,
                                          LocalDateTime fetchedAt, LocalDateTime normalizedAt, String normalizationError) {

        StagingRawFhirResourceDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (resourceType != null) record.setResourceType(resourceType);
            if (fhirResourceId != null) record.setFhirResourceId(fhirResourceId);
            if (rawPayload != null) record.setRawPayload(rawPayload);
            if (fetchedAt != null) record.setFetchedAt(fetchedAt);
            if (normalizedAt != null) record.setNormalizedAt(normalizedAt);
            if (normalizationError != null) record.setNormalizationError(normalizationError);
            record.setUpdatedAt(LocalDateTime.now());

            StagingRawFhirResourceDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null;
        }
    }

    public boolean delete(@NonNull String extid) {

        StagingRawFhirResourceDb record = repository.findByExtid(extid)
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

    public StagingRawFhirResource findByExtid(@NonNull String extid) {
        StagingRawFhirResourceDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public StagingRawFhirResource refreshForRenormalization(@NonNull String extid, @NonNull String rawPayload,
                                                             @NonNull LocalDateTime fetchedAt) {
        StagingRawFhirResourceDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            record.setRawPayload(rawPayload);
            record.setFetchedAt(fetchedAt);
            record.setNormalizedAt(null);
            record.setNormalizationError(null);
            record.setUpdatedAt(LocalDateTime.now());

            StagingRawFhirResourceDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null;
        }
    }

    public StagingRawFhirResource findByResourceTypeAndFhirResourceId(@NonNull String resourceType, @NonNull String fhirResourceId) {
        return repository.findByResourceTypeAndFhirResourceId(resourceType, fhirResourceId)
                .map(mapper::toModel)
                .orElse(null);
    }

    public List<StagingRawFhirResource> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public List<StagingRawFhirResource> findPending(int maxRows) {
        List<StagingRawFhirResourceDb> records = repository.findByNormalizedAtIsNullAndActive(
                ActiveEnum.ACTIVE, PageRequest.of(0, maxRows));
        return findAndLog(records, String.format("pending (max %d)", maxRows));
    }

    public Page<StagingRawFhirResource> findAll(Pageable pageable) {
        Page<StagingRawFhirResourceDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<StagingRawFhirResource> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<StagingRawFhirResource> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<StagingRawFhirResourceDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    private List<StagingRawFhirResource> findAndLog(List<StagingRawFhirResourceDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
