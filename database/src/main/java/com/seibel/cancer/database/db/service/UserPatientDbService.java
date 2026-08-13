package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.UserPatient;
import com.seibel.cancer.common.enums.AccessLevel;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.entity.UserPatientDb;
import com.seibel.cancer.database.db.mapper.UserPatientMapper;
import com.seibel.cancer.database.db.repository.UserPatientRepository;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Grants. Deliberately has no controller - access is managed through purpose-built
 * grant/revoke endpoints in step 8, not generic CRUD, because "PUT a row on the
 * authorisation table" is not an operation anyone should be able to perform directly.
 */
@Slf4j
@Service
public class UserPatientDbService extends BaseDbService {

    private final UserPatientRepository repository;
    private final UserPatientMapper mapper;

    public UserPatientDbService(UserPatientRepository repository, UserPatientMapper mapper) {
        super("UserPatientDb");
        this.repository = repository;
        this.mapper = mapper;
    }

    public UserPatient create(@NonNull UserPatient item) {

        String extid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        try {
            UserPatientDb record = mapper.toDb(item);
            record.setExtid(extid);
            record.setCreatedAt(now);
            record.setUpdatedAt(now);
            record.setActive(ActiveEnum.ACTIVE);
            // A grant is in force from the moment it exists unless told otherwise.
            if (record.getGrantedAt() == null) record.setGrantedAt(now);

            UserPatientDb saved = repository.save(record);
            log.info(getCreatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("create", extid, e);
            return null; // unreachable
        }
    }

    /**
     * Grant one login access to one patient. The named form of {@link #create}.
     *
     * <p><strong>Re-granting to a pair that already has an active grant updates that grant's
     * level rather than creating a second row.</strong> There is no database unique constraint
     * on {@code (user_id, patient_id)} - there cannot be a useful one, since revoked rows are
     * kept and a re-grant after a revoke is legitimate - so uniqueness among <em>active</em>
     * grants is enforced here. Without this, granting twice would leave two live rows for the
     * same pair and the level in force would depend on which one a query happened to return.
     */
    public UserPatient grant(@NonNull Long userId, @NonNull Long patientId,
                             @NonNull AccessLevel accessLevel, Long grantedByUserId, String note) {

        Optional<UserPatientDb> existing = repository.findActiveGrant(userId, patientId);
        if (existing.isPresent()) {
            UserPatientDb record = existing.get();
            log.info("Active grant already exists for userId={} patientId={}; updating level {} -> {}",
                    userId, patientId, record.getAccessLevel(), accessLevel);
            return update(record.getExtid(), UserPatient.builder()
                    .accessLevel(accessLevel)
                    .note(note)
                    .build());
        }

        return create(UserPatient.builder()
                .userId(userId)
                .patientId(patientId)
                .accessLevel(accessLevel)
                .grantedByUserId(grantedByUserId)
                .grantedAt(LocalDateTime.now())
                .note(note)
                .build());
    }

    public UserPatient update(@NonNull String extid, @NonNull UserPatient item) {

        UserPatientDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (item.getUserId() != null) record.setUserId(item.getUserId());
            if (item.getPatientId() != null) record.setPatientId(item.getPatientId());
            if (item.getAccessLevel() != null) record.setAccessLevel(item.getAccessLevel());
            if (item.getGrantedByUserId() != null) record.setGrantedByUserId(item.getGrantedByUserId());
            if (item.getGrantedAt() != null) record.setGrantedAt(item.getGrantedAt());
            if (item.getRevokedAt() != null) record.setRevokedAt(item.getRevokedAt());
            if (item.getNote() != null) record.setNote(item.getNote());
            record.setUpdatedAt(LocalDateTime.now());

            UserPatientDb saved = repository.save(record);
            log.info(getUpdatedMessage(extid));
            return mapper.toModel(saved);

        } catch (Exception e) {
            handleException("update", extid, e);
            return null; // unreachable
        }
    }

    /**
     * End a grant by stamping {@code revokedAt}.
     *
     * <p>Not a delete: who had access to a medical record, and when, is the history worth
     * keeping. Re-revoking an already-revoked grant leaves the original timestamp alone, so
     * the record reflects when access actually ended.
     */
    public UserPatient revoke(@NonNull String extid) {

        UserPatientDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        try {
            if (record.getRevokedAt() == null) {
                record.setRevokedAt(LocalDateTime.now());
                record.setUpdatedAt(LocalDateTime.now());
                record = repository.save(record);
            }
            log.info("UserPatientDb with extid={} revoked", extid);
            return mapper.toModel(record);

        } catch (Exception e) {
            handleException("revoke", extid, e);
            return null; // unreachable
        }
    }

    public boolean delete(@NonNull String extid) {

        UserPatientDb record = repository.findByExtid(extid)
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

    public UserPatient findByExtid(@NonNull String extid) {

        UserPatientDb record = repository.findByExtid(extid)
                .orElseThrow(() -> new ServiceException(getFoundFailureMessage(extid)));

        log.info(getFoundMessage(extid));
        return mapper.toModel(record);
    }

    public List<UserPatient> findAll() {
        return findAndLog(repository.findAllActive(), "findAll");
    }

    public Page<UserPatient> findAll(Pageable pageable) {
        return repository.findByActive(ActiveEnum.ACTIVE, pageable).map(mapper::toModel);
    }

    public List<UserPatient> findByActive(@NonNull ActiveEnum activeEnum) {
        return findAndLog(repository.findByActive(activeEnum), String.format("active (%s)", activeEnum));
    }

    public Page<UserPatient> findByActive(@NonNull ActiveEnum activeEnum, Pageable pageable) {
        return repository.findByActive(activeEnum, pageable).map(mapper::toModel);
    }

    /** Every patient this login may currently see. */
    public List<UserPatient> findActiveGrantsForUser(@NonNull Long userId) {
        return findAndLog(repository.findActiveGrantsForUser(userId),
                String.format("active grants for userId (%d)", userId));
    }

    /** Everyone who may currently see this patient. */
    public List<UserPatient> findActiveGrantsForPatient(@NonNull Long patientId) {
        return findAndLog(repository.findActiveGrantsForPatient(patientId),
                String.format("active grants for patientId (%d)", patientId));
    }

    /**
     * The grant linking one login to one patient, if it is in force.
     *
     * <p>Returns {@link Optional#empty()} rather than throwing: "no grant" is the ordinary
     * answer for most user/patient pairs, not an exceptional one. The authorisation check in
     * root turns an empty result into a 404.
     */
    public Optional<UserPatient> findActiveGrant(@NonNull Long userId, @NonNull Long patientId) {
        return repository.findActiveGrant(userId, patientId).map(mapper::toModel);
    }

    private List<UserPatient> findAndLog(List<UserPatientDb> records, String type) {
        log.info(getFoundMessageByType(type, records.size()));
        return mapper.toModelList(records);
    }
}
