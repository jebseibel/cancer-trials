package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.UcHealthOAuthToken;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.UcHealthOAuthTokenDb;
import com.seibel.cancer.database.db.mapper.UcHealthOAuthTokenMapper;
import com.seibel.cancer.database.db.repository.UcHealthOAuthTokenRepository;
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
public class UcHealthOAuthTokenDbService extends BaseDbService {

    private final UcHealthOAuthTokenRepository repository;
    private final UcHealthOAuthTokenMapper mapper;

    public UcHealthOAuthTokenDbService(UcHealthOAuthTokenRepository repository, UcHealthOAuthTokenMapper mapper) {
        super("UcHealthOAuthTokenDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public UcHealthOAuthToken create(@NonNull UcHealthOAuthToken item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            UcHealthOAuthTokenDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            UcHealthOAuthTokenDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null;
        }
    }

    public UcHealthOAuthToken create(String accessToken, String refreshToken, LocalDateTime expiresAt,
                                      String patientFhirId, String scope) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            UcHealthOAuthTokenDb record = new UcHealthOAuthTokenDb();
            record.setExtid(extid);
            record.setAccessToken(accessToken);
            record.setRefreshToken(refreshToken);
            record.setExpiresAt(expiresAt);
            record.setPatientFhirId(patientFhirId);
            record.setScope(scope);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);

            UcHealthOAuthTokenDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    public UcHealthOAuthToken update(@NonNull String extid, String accessToken, String refreshToken,
                                      LocalDateTime expiresAt, String patientFhirId, String scope) {

        UcHealthOAuthTokenDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (accessToken != null) record.setAccessToken(accessToken);
            if (refreshToken != null) record.setRefreshToken(refreshToken);
            if (expiresAt != null) record.setExpiresAt(expiresAt);
            if (patientFhirId != null) record.setPatientFhirId(patientFhirId);
            if (scope != null) record.setScope(scope);
            record.setUpdatedAt(LocalDateTime.now());

            UcHealthOAuthTokenDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null;
        }
    }

    public boolean delete(@NonNull String extid) {

        UcHealthOAuthTokenDb record = repository.findByExtid(extid)
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

    public UcHealthOAuthToken findByExtid(@NonNull String extid) {
        UcHealthOAuthTokenDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));
        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    /** Returns the most recently created active token, or null if none exists yet (not yet authorized). */
    public UcHealthOAuthToken findCurrent() {
        return repository.findFirstByActiveOrderByCreatedAtDesc(ActiveEnum.ACTIVE)
                .map(mapper::toModel)
                .orElse(null);
    }

    public List<UcHealthOAuthToken> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<UcHealthOAuthToken> findAll(Pageable pageable) {
        Page<UcHealthOAuthTokenDb> page = repository.findByActive(ActiveEnum.ACTIVE, pageable);
        log.info(getFoundMessageByType("findAll(pageable)", (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    public List<UcHealthOAuthToken> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum),
                String.format("active (%s)", activeEnum));
    }

    public Page<UcHealthOAuthToken> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        Page<UcHealthOAuthTokenDb> page = repository.findByActive(activeEnum, pageable);
        log.info(getFoundMessageByType(String.format("active (%s) pageable", activeEnum), (int) page.getTotalElements()));
        return page.map(mapper::toModel);
    }

    private List<UcHealthOAuthToken> findAndLog(List<UcHealthOAuthTokenDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }

}
