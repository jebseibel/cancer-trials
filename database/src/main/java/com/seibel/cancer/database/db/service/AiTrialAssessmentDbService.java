package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.AiTrialAssessment;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.entity.AiTrialAssessmentDb;
import com.seibel.cancer.database.db.mapper.AiTrialAssessmentMapper;
import com.seibel.cancer.database.db.repository.AiTrialAssessmentRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Stored readings of a trial against a patient's record.
 *
 * <p><b>Insert-only in practice.</b> There is no update: a re-check writes a new row, because the
 * point of keeping these is being able to see what she was told and when. Overwriting would
 * destroy the only record of an answer she may already have acted on.
 */
@Slf4j
@Service
public class AiTrialAssessmentDbService extends BaseDbService {

    private final AiTrialAssessmentRepository repository;
    private final AiTrialAssessmentMapper mapper;

    public AiTrialAssessmentDbService(AiTrialAssessmentRepository repository,
                                      AiTrialAssessmentMapper mapper) {
        super("AiTrialAssessmentDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public AiTrialAssessment create(@NonNull AiTrialAssessment item) {
        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            AiTrialAssessmentDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);
            if (record.getAssessedAt() == null) {
                record.setAssessedAt(now);
            }

            AiTrialAssessmentDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);
        } catch (Exception e) {
            String message = getOperationFailureMessage("Create", extid);
            log.error(message, e);
            throw new ServiceException(message, e);
        }
    }

    /** Every reading of this trial for this patient, newest first. Empty when never run. */
    public List<AiTrialAssessment> findByTrialAndPatient(@NonNull Long trialId,
                                                         @NonNull Long patientId) {
        return mapper.toModelList(
                repository.findByTrialIdAndPatientIdAndActiveOrderByAssessedAtDesc(
                        trialId, patientId, ActiveEnum.ACTIVE));
    }

    /**
     * The most recent reading, or null.
     *
     * <p>What the trial page shows on open: she sees what she was told last time, dated, and
     * re-running is a deliberate press rather than something that happens by arriving.
     */
    public AiTrialAssessment findLatest(@NonNull Long trialId, @NonNull Long patientId) {
        List<AiTrialAssessment> all = findByTrialAndPatient(trialId, patientId);
        return all.isEmpty() ? null : all.getFirst();
    }
}
