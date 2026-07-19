package com.seibel.jobs.database.db.service;

import com.seibel.jobs.common.domain.Friend;
import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.common.exceptions.ServiceException;
import com.seibel.jobs.database.db.entity.FriendDb;
import com.seibel.jobs.database.db.mapper.FriendMapper;
import com.seibel.jobs.database.db.repository.FriendRepository;
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
public class FriendDbService extends BaseDbService {

    private final FriendRepository repository;
    private final FriendMapper mapper;

    public FriendDbService(FriendRepository repository, FriendMapper mapper) {
        super("FriendDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public Friend create(@NonNull Friend item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            FriendDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            FriendDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null;
        }
    }

    public Friend create(@NonNull String name, String relationship, String email, String phone,
                          String linkedinUrl, LocalDate lastContactedAt, String notes) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            FriendDb record = new FriendDb();
            record.setExtid(extid);
            record.setName(name);
            record.setRelationship(relationship);
            record.setEmail(email);
            record.setPhone(phone);
            record.setLinkedinUrl(linkedinUrl);
            record.setLastContactedAt(lastContactedAt);
            record.setNotes(notes);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            FriendDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public Friend update(@NonNull String extid, String name, String relationship, String email, String phone,
                          String linkedinUrl, LocalDate lastContactedAt, String notes) {

        FriendDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (name != null) record.setName(name);
            if (relationship != null) record.setRelationship(relationship);
            if (email != null) record.setEmail(email);
            if (phone != null) record.setPhone(phone);
            if (linkedinUrl != null) record.setLinkedinUrl(linkedinUrl);
            if (lastContactedAt != null) record.setLastContactedAt(lastContactedAt);
            if (notes != null) record.setNotes(notes);
            record.setUpdatedAt(LocalDateTime.now());

            FriendDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null;
        }
    }

    public boolean delete(@NonNull String extid) {

        FriendDb record = repository.findByExtid(extid)
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

    public Friend findByExtid(@NonNull String extid) {
        FriendDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<Friend> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<Friend> findAll(Pageable pageable) {
        Page<FriendDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<Friend> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<Friend> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<FriendDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    private List<Friend> findAndLog(List<FriendDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
