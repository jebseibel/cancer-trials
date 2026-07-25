package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.Sponsor;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.SponsorDb;
import com.seibel.cancer.database.db.mapper.SponsorMapper;
import com.seibel.cancer.database.db.repository.SponsorRepository;
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
public class SponsorDbService extends BaseDbService {

    private final SponsorRepository repository;
    private final SponsorMapper mapper;

    public SponsorDbService(SponsorRepository repository, SponsorMapper mapper) {
        super("SponsorDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public Sponsor create(@NonNull Sponsor item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            SponsorDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            SponsorDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public Sponsor create(@NonNull String name, String orgClass) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            SponsorDb record = new SponsorDb();
            record.setExtid(extid);
            record.setName(name);
            record.setOrgClass(orgClass);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            SponsorDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public Sponsor update(@NonNull String extid, String name, String orgClass) {

        SponsorDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (name != null) record.setName(name);
            if (orgClass != null) record.setOrgClass(orgClass);
            record.setUpdatedAt(LocalDateTime.now());

            SponsorDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null; // unreachable
        }
    }

    public boolean delete(@NonNull String extid) {

        SponsorDb record = repository.findByExtid(extid)
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

    public Sponsor findByExtid(@NonNull String extid) {
        SponsorDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<Sponsor> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<Sponsor> findAll(Pageable pageable) {
        Page<SponsorDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<Sponsor> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<Sponsor> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<SponsorDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    private List<Sponsor> findAndLog(List<SponsorDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
