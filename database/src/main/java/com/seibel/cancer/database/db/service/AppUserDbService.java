package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.AppUser;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.AppUserDb;
import com.seibel.cancer.database.db.mapper.AppUserMapper;
import com.seibel.cancer.database.db.repository.AppUserRepository;
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
public class AppUserDbService extends BaseDbService {

    private final AppUserRepository repository;
    private final AppUserMapper mapper;

    public AppUserDbService(AppUserRepository repository, AppUserMapper mapper) {
        super("AppUserDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public AppUser create(@NonNull AppUser item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            AppUserDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            AppUserDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public AppUser create(@NonNull String username, @NonNull String passwordHash, String displayName) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            AppUserDb record = new AppUserDb();
            record.setExtid(extid);
            record.setUsername(username);
            record.setPasswordHash(passwordHash);
            record.setDisplayName(displayName);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            AppUserDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public AppUser update(@NonNull String extid, String username, String passwordHash, String displayName) {

        AppUserDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (username != null) record.setUsername(username);
            if (passwordHash != null) record.setPasswordHash(passwordHash);
            if (displayName != null) record.setDisplayName(displayName);
            record.setUpdatedAt(LocalDateTime.now());

            AppUserDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null; // unreachable
        }
    }

    public boolean delete(@NonNull String extid) {

        AppUserDb record = repository.findByExtid(extid)
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

    public AppUser findByExtid(@NonNull String extid) {
        AppUserDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<AppUser> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<AppUser> findAll(Pageable pageable) {
        Page<AppUserDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<AppUser> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<AppUser> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<AppUserDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    private List<AppUser> findAndLog(List<AppUserDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
