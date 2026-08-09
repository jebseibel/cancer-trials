package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.SavedTrialMatchCriterion;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.entity.SavedTrialMatchCriterionDb;
import com.seibel.cancer.database.db.mapper.SavedTrialMatchCriterionMapper;
import com.seibel.cancer.database.db.repository.SavedTrialMatchCriterionRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class SavedTrialMatchCriterionDbService extends BaseDbService {

    private final SavedTrialMatchCriterionRepository repository;
    private final SavedTrialMatchCriterionMapper mapper;

    public SavedTrialMatchCriterionDbService(SavedTrialMatchCriterionRepository repository,
                                        SavedTrialMatchCriterionMapper mapper) {
        super("SavedTrialMatchCriterionDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public SavedTrialMatchCriterion create(@NonNull SavedTrialMatchCriterion item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            SavedTrialMatchCriterionDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            SavedTrialMatchCriterionDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public SavedTrialMatchCriterion create(@NonNull Long trialMatchId, @NonNull String chunkText,
                                      @NonNull BigDecimal score, Boolean isExclusion,
                                      String source, Integer ordinal) {
        return create(SavedTrialMatchCriterion.builder()
                .trialMatchId(trialMatchId)
                .chunkText(chunkText)
                .score(score)
                .isExclusion(isExclusion)
                .source(source)
                .ordinal(ordinal)
                .build());
    }

    public SavedTrialMatchCriterion update(@NonNull String extid, @NonNull SavedTrialMatchCriterion item) {

        SavedTrialMatchCriterionDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (item.getTrialMatchId() != null) record.setTrialMatchId(item.getTrialMatchId());
            if (item.getChunkText() != null) record.setChunkText(item.getChunkText());
            if (item.getScore() != null) record.setScore(item.getScore());
            if (item.getIsExclusion() != null) record.setIsExclusion(item.getIsExclusion());
            if (item.getSource() != null) record.setSource(item.getSource());
            if (item.getOrdinal() != null) record.setOrdinal(item.getOrdinal());
            record.setUpdatedAt(LocalDateTime.now());

            SavedTrialMatchCriterionDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null; // unreachable
        }
    }

    public boolean delete(@NonNull String extid) {

        SavedTrialMatchCriterionDb record = repository.findByExtid(extid)
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

    public SavedTrialMatchCriterion findByExtid(@NonNull String extid) {

        SavedTrialMatchCriterionDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<SavedTrialMatchCriterion> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<SavedTrialMatchCriterion> findAll(Pageable pageable) {
        return repository.findByActive(ActiveEnum.ACTIVE, pageable).map(mapper::toModel);
    }

    public List<SavedTrialMatchCriterion> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum), String.format("active (%s)", activeEnum));
    }

    public Page<SavedTrialMatchCriterion> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        return repository.findByActive(activeEnum, pageable).map(mapper::toModel);
    }

    public List<SavedTrialMatchCriterion> findByTrialMatchId(@NonNull Long trialMatchId) {
        return findAndLog(repository.findByTrialMatchId(trialMatchId),
                String.format("trialMatchId (%d)", trialMatchId));
    }

    private List<SavedTrialMatchCriterion> findAndLog(List<SavedTrialMatchCriterionDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }
}
