package com.seibel.jobs.database.db.service;

import com.seibel.jobs.common.domain.FriendJobPosting;
import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.common.exceptions.ServiceException;
import com.seibel.jobs.database.db.entity.FriendJobPostingDb;
import com.seibel.jobs.database.db.mapper.FriendJobPostingMapper;
import com.seibel.jobs.database.db.repository.FriendJobPostingRepository;
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
public class FriendJobPostingDbService extends BaseDbService {

    private final FriendJobPostingRepository repository;
    private final FriendJobPostingMapper mapper;

    public FriendJobPostingDbService(FriendJobPostingRepository repository, FriendJobPostingMapper mapper) {
        super("FriendJobPostingDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public FriendJobPosting create(@NonNull FriendJobPosting item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            FriendJobPostingDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            FriendJobPostingDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null;
        }
    }

    public FriendJobPosting create(@NonNull Long friendId, @NonNull Long jobPostingId) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            FriendJobPostingDb record = new FriendJobPostingDb();
            record.setExtid(extid);
            record.setFriendId(friendId);
            record.setJobPostingId(jobPostingId);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            FriendJobPostingDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public FriendJobPosting update(@NonNull String extid, Long friendId, Long jobPostingId) {

        FriendJobPostingDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (friendId != null) record.setFriendId(friendId);
            if (jobPostingId != null) record.setJobPostingId(jobPostingId);
            record.setUpdatedAt(LocalDateTime.now());

            FriendJobPostingDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null;
        }
    }

    public boolean delete(@NonNull String extid) {

        FriendJobPostingDb record = repository.findByExtid(extid)
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

    public FriendJobPosting findByExtid(@NonNull String extid) {
        FriendJobPostingDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<FriendJobPosting> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<FriendJobPosting> findAll(Pageable pageable) {
        Page<FriendJobPostingDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<FriendJobPosting> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<FriendJobPosting> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<FriendJobPostingDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<FriendJobPosting> findByFriendId(@NonNull Long friendId) {
        return findAndLog(repository.findByFriendId(friendId), String.format("friendId (%s)", friendId));
    }

    public List<FriendJobPosting> findByJobPostingId(@NonNull Long jobPostingId) {
        return findAndLog(repository.findByJobPostingId(jobPostingId), String.format("jobPostingId (%s)", jobPostingId));
    }

    private List<FriendJobPosting> findAndLog(List<FriendJobPostingDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
