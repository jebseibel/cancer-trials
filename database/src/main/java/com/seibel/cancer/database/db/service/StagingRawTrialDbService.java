package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.StagingRawTrial;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.StagingRawTrialDb;
import com.seibel.cancer.database.db.mapper.StagingRawTrialMapper;
import com.seibel.cancer.database.db.repository.StagingRawTrialRepository;
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
public class StagingRawTrialDbService extends BaseDbService {

    private final StagingRawTrialRepository repository;
    private final StagingRawTrialMapper mapper;

    public StagingRawTrialDbService(StagingRawTrialRepository repository, StagingRawTrialMapper mapper) {
        super("StagingRawTrialDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public StagingRawTrial create(@NonNull StagingRawTrial item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            StagingRawTrialDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            StagingRawTrialDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null;
        }
    }

    public StagingRawTrial create(@NonNull Long trialSourceId, @NonNull String sourceTrialId, String rawPayload,
                                   @NonNull LocalDateTime fetchedAt, LocalDateTime normalizedAt, String normalizationError) {
        return create(trialSourceId, sourceTrialId, rawPayload, null, fetchedAt, normalizedAt, normalizationError);
    }

    /**
     * @param payloadHash SHA-256 hex of rawPayload, or null when unknown. Always written
     *                    together with the payload so the two can never describe different
     *                    content.
     */
    public StagingRawTrial create(@NonNull Long trialSourceId, @NonNull String sourceTrialId, String rawPayload,
                                   String payloadHash, @NonNull LocalDateTime fetchedAt,
                                   LocalDateTime normalizedAt, String normalizationError) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            StagingRawTrialDb record = new StagingRawTrialDb();
            record.setExtid(extid);
            record.setTrialSourceId(trialSourceId);
            record.setSourceTrialId(sourceTrialId);
            record.setRawPayload(rawPayload);
            record.setPayloadHash(payloadHash);
            record.setFetchedAt(fetchedAt);
            record.setNormalizedAt(normalizedAt);
            record.setNormalizationError(normalizationError);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            StagingRawTrialDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public StagingRawTrial update(@NonNull String extid, Long trialSourceId, String sourceTrialId, String rawPayload,
                                   LocalDateTime fetchedAt, LocalDateTime normalizedAt, String normalizationError) {

        StagingRawTrialDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (trialSourceId != null) record.setTrialSourceId(trialSourceId);
            if (sourceTrialId != null) record.setSourceTrialId(sourceTrialId);
            if (rawPayload != null) record.setRawPayload(rawPayload);
            if (fetchedAt != null) record.setFetchedAt(fetchedAt);
            if (normalizedAt != null) record.setNormalizedAt(normalizedAt);
            if (normalizationError != null) record.setNormalizationError(normalizationError);
            record.setUpdatedAt(LocalDateTime.now());

            StagingRawTrialDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null;
        }
    }

    public boolean delete(@NonNull String extid) {

        StagingRawTrialDb record = repository.findByExtid(extid)
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

    public StagingRawTrial findByExtid(@NonNull String extid) {
        StagingRawTrialDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public StagingRawTrial refreshForRenormalization(@NonNull String extid, @NonNull String rawPayload,
                                                      @NonNull LocalDateTime fetchedAt) {
        return refreshForRenormalization(extid, rawPayload, null, fetchedAt);
    }

    /**
     * Replaces the stored payload and re-queues the row for normalization.
     *
     * @param payloadHash SHA-256 hex of the new rawPayload. Set here rather than left alone on
     *                    purpose: a row carrying a hash of its *previous* payload would cause
     *                    the next pull to compare against a stale value and skip a trial that
     *                    had in fact changed.
     */
    public StagingRawTrial refreshForRenormalization(@NonNull String extid, @NonNull String rawPayload,
                                                      String payloadHash, @NonNull LocalDateTime fetchedAt) {
        StagingRawTrialDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            record.setRawPayload(rawPayload);
            record.setPayloadHash(payloadHash);
            record.setFetchedAt(fetchedAt);
            record.setNormalizedAt(null);
            record.setNormalizationError(null);
            record.setUpdatedAt(LocalDateTime.now());

            StagingRawTrialDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null;
        }
    }

    public StagingRawTrial findByTrialSourceIdAndSourceTrialId(@NonNull Long trialSourceId, @NonNull String sourceTrialId) {
        return repository.findByTrialSourceIdAndSourceTrialId(trialSourceId, sourceTrialId)
                .map(mapper::toModel)
                .orElse(null);
    }

    public List<StagingRawTrial> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public List<StagingRawTrial> findPending(int maxRows) {
        List<StagingRawTrialDb> records = repository.findByNormalizedAtIsNullAndActive(
                ActiveEnum.ACTIVE, PageRequest.of(0, maxRows));
        return findAndLog(records, String.format("pending (max %d)", maxRows));
    }

    public Page<StagingRawTrial> findAll(Pageable pageable) {
        Page<StagingRawTrialDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<StagingRawTrial> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<StagingRawTrial> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<StagingRawTrialDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    private List<StagingRawTrial> findAndLog(List<StagingRawTrialDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
