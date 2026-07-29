package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.EligibilityRule;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.EligibilityRuleDb;
import com.seibel.cancer.database.db.mapper.EligibilityRuleMapper;
import com.seibel.cancer.database.db.repository.EligibilityRuleRepository;
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
public class EligibilityRuleDbService extends BaseDbService {

    private final EligibilityRuleRepository repository;
    private final EligibilityRuleMapper mapper;

    public EligibilityRuleDbService(EligibilityRuleRepository repository, EligibilityRuleMapper mapper) {
        super("EligibilityRuleDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public EligibilityRule create(@NonNull EligibilityRule item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            EligibilityRuleDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            EligibilityRuleDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public EligibilityRule update(@NonNull String extid, EligibilityRule item) {

        EligibilityRuleDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (item.getTrialId() != null) record.setTrialId(item.getTrialId());
            if (item.getParentRuleId() != null) record.setParentRuleId(item.getParentRuleId());
            if (item.getNodeType() != null) record.setNodeType(item.getNodeType());
            if (item.getOperator() != null) record.setOperator(item.getOperator());
            if (item.getCriterionType() != null) record.setCriterionType(item.getCriterionType());
            if (item.getCriterionId() != null) record.setCriterionId(item.getCriterionId());
            if (item.getRequirementType() != null) record.setRequirementType(item.getRequirementType());
            if (item.getSortOrder() != null) record.setSortOrder(item.getSortOrder());
            if (item.getNotes() != null) record.setNotes(item.getNotes());
            record.setUpdatedAt(LocalDateTime.now());

            EligibilityRuleDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null; // unreachable
        }
    }

    public boolean delete(@NonNull String extid) {

        EligibilityRuleDb record = repository.findByExtid(extid)
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

    public EligibilityRule findByExtid(@NonNull String extid) {
        EligibilityRuleDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<EligibilityRule> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public List<EligibilityRule> findByTrialId(@NonNull Long trialId) {
        return findAndLog(repository.findByTrialId(trialId), String.format("trialId (%d)", trialId));
    }

    public Page<EligibilityRule> findAll(Pageable pageable) {
        Page<EligibilityRuleDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<EligibilityRule> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<EligibilityRule> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<EligibilityRuleDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    private List<EligibilityRule> findAndLog(List<EligibilityRuleDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
