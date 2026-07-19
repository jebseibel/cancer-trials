package com.seibel.jobs.database.db.service;

import com.seibel.jobs.common.domain.FriendCompany;
import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.common.exceptions.ServiceException;
import com.seibel.jobs.database.db.entity.FriendCompanyDb;
import com.seibel.jobs.database.db.mapper.FriendCompanyMapper;
import com.seibel.jobs.database.db.repository.FriendCompanyRepository;
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
public class FriendCompanyDbService extends BaseDbService {

    private final FriendCompanyRepository repository;
    private final FriendCompanyMapper mapper;

    public FriendCompanyDbService(FriendCompanyRepository repository, FriendCompanyMapper mapper) {
        super("FriendCompanyDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public FriendCompany create(@NonNull FriendCompany item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            FriendCompanyDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            FriendCompanyDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null;
        }
    }

    public FriendCompany create(@NonNull Long friendId, @NonNull Long companyId) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            FriendCompanyDb record = new FriendCompanyDb();
            record.setExtid(extid);
            record.setFriendId(friendId);
            record.setCompanyId(companyId);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            FriendCompanyDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public FriendCompany update(@NonNull String extid, Long friendId, Long companyId) {

        FriendCompanyDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (friendId != null) record.setFriendId(friendId);
            if (companyId != null) record.setCompanyId(companyId);
            record.setUpdatedAt(LocalDateTime.now());

            FriendCompanyDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null;
        }
    }

    public boolean delete(@NonNull String extid) {

        FriendCompanyDb record = repository.findByExtid(extid)
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

    public FriendCompany findByExtid(@NonNull String extid) {
        FriendCompanyDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<FriendCompany> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<FriendCompany> findAll(Pageable pageable) {
        Page<FriendCompanyDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<FriendCompany> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<FriendCompany> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<FriendCompanyDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<FriendCompany> findByFriendId(@NonNull Long friendId) {
        return findAndLog(repository.findByFriendId(friendId), String.format("friendId (%s)", friendId));
    }

    public List<FriendCompany> findByCompanyId(@NonNull Long companyId) {
        return findAndLog(repository.findByCompanyId(companyId), String.format("companyId (%s)", companyId));
    }

    private List<FriendCompany> findAndLog(List<FriendCompanyDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
