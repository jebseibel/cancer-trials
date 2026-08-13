package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.UserPatient;
import com.seibel.cancer.common.enums.AccessLevel;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.entity.UserPatientDb;
import com.seibel.cancer.database.db.mapper.UserPatientMapper;
import com.seibel.cancer.database.db.repository.UserPatientRepository;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPatientDbServiceTest {

    private static final Long USER_ID = 4001L;
    private static final Long PATIENT_ID = 5001L;

    @Mock
    private UserPatientRepository repository;

    @Mock
    private UserPatientMapper mapper;

    @InjectMocks
    private UserPatientDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        UserPatient input = DomainBuilderDatabase.getUserPatient();
        UserPatientDb mapped = DomainBuilderDatabase.getUserPatientDb();

        when(mapper.toDb(input)).thenReturn(mapped);
        when(repository.save(any(UserPatientDb.class))).thenReturn(mapped);
        when(mapper.toModel(mapped)).thenReturn(input);

        assertNotNull(service.create(input));

        ArgumentCaptor<UserPatientDb> captor = ArgumentCaptor.forClass(UserPatientDb.class);
        verify(repository).save(captor.capture());
        UserPatientDb saved = captor.getValue();

        assertNotNull(saved.getExtid());
        assertEquals(ActiveEnum.ACTIVE, saved.getActive());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getGrantedAt());
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        UserPatient input = DomainBuilderDatabase.getUserPatient();

        when(mapper.toDb(input)).thenReturn(DomainBuilderDatabase.getUserPatientDb());
        when(repository.save(any(UserPatientDb.class))).thenThrow(new RuntimeException("boom"));

        assertThrows(ServiceException.class, () -> service.create(input));
    }

    @Test
    void grant_shouldCreateNewGrant_whenNoneExists() {
        when(repository.findActiveGrant(USER_ID, PATIENT_ID)).thenReturn(Optional.empty());
        when(mapper.toDb(any(UserPatient.class))).thenReturn(DomainBuilderDatabase.getUserPatientDb());
        when(repository.save(any(UserPatientDb.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toModel(any(UserPatientDb.class)))
                .thenReturn(DomainBuilderDatabase.getUserPatient());

        service.grant(USER_ID, PATIENT_ID, AccessLevel.VIEW_TRIALS, 1L, "my sister");

        verify(repository).save(any(UserPatientDb.class));
    }

    /**
     * There is no database unique constraint on (user_id, patient_id) - there cannot be a
     * useful one while revoked rows are kept - so a second grant must not create a second
     * live row, or the level in force would depend on which row a query returned.
     */
    @Test
    void grant_shouldUpdateExistingGrant_ratherThanCreatingASecondLiveRow() {
        String extid = UUID.randomUUID().toString();
        UserPatientDb existing = DomainBuilderDatabase.getUserPatientDb(
                USER_ID, PATIENT_ID, AccessLevel.VIEW_TRIALS, extid);

        when(repository.findActiveGrant(USER_ID, PATIENT_ID)).thenReturn(Optional.of(existing));
        when(repository.findByExtid(extid)).thenReturn(Optional.of(existing));
        when(repository.save(any(UserPatientDb.class))).thenReturn(existing);
        when(mapper.toModel(existing)).thenReturn(DomainBuilderDatabase.getUserPatient(existing));

        service.grant(USER_ID, PATIENT_ID, AccessLevel.EDIT_RECORD, 1L, "upgraded");

        ArgumentCaptor<UserPatientDb> captor = ArgumentCaptor.forClass(UserPatientDb.class);
        verify(repository).save(captor.capture());

        assertEquals(AccessLevel.EDIT_RECORD, captor.getValue().getAccessLevel());
        // The existing row was updated - mapper.toDb would mean a new row was built instead.
        verify(mapper, never()).toDb(any(UserPatient.class));
    }

    @Test
    void revoke_shouldSetRevokedAt() {
        String extid = UUID.randomUUID().toString();
        UserPatientDb existing = DomainBuilderDatabase.getUserPatientDb(
                USER_ID, PATIENT_ID, AccessLevel.VIEW_RECORD, extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existing));
        when(repository.save(any(UserPatientDb.class))).thenReturn(existing);
        when(mapper.toModel(existing)).thenReturn(DomainBuilderDatabase.getUserPatient(existing));

        service.revoke(extid);

        ArgumentCaptor<UserPatientDb> captor = ArgumentCaptor.forClass(UserPatientDb.class);
        verify(repository).save(captor.capture());

        assertNotNull(captor.getValue().getRevokedAt());
        // Revocation is not a delete: the row stays active and undeleted.
        assertEquals(ActiveEnum.ACTIVE, captor.getValue().getActive());
    }

    /** Re-revoking must not move the timestamp, or the record of when access ended is lost. */
    @Test
    void revoke_shouldLeaveAnAlreadyRevokedGrantUntouched() {
        String extid = UUID.randomUUID().toString();
        LocalDateTime originallyRevoked = LocalDateTime.now().minusDays(3);
        UserPatientDb existing = DomainBuilderDatabase.getUserPatientDb(
                USER_ID, PATIENT_ID, AccessLevel.VIEW_RECORD, extid);
        existing.setRevokedAt(originallyRevoked);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existing));
        when(mapper.toModel(existing)).thenReturn(DomainBuilderDatabase.getUserPatient(existing));

        service.revoke(extid);

        verify(repository, never()).save(any(UserPatientDb.class));
        assertEquals(originallyRevoked, existing.getRevokedAt());
    }

    @Test
    void revoke_shouldThrowException_whenNotFound() {
        String extid = UUID.randomUUID().toString();
        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        assertThrows(ServiceException.class, () -> service.revoke(extid));
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        String extid = UUID.randomUUID().toString();
        UserPatientDb existing = DomainBuilderDatabase.getUserPatientDb(
                USER_ID, PATIENT_ID, AccessLevel.VIEW_TRIALS, extid);
        UserPatient update = UserPatient.builder().accessLevel(AccessLevel.OWNER).build();

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existing));
        when(repository.save(any(UserPatientDb.class))).thenReturn(existing);
        when(mapper.toModel(existing)).thenReturn(update);

        service.update(extid, update);

        ArgumentCaptor<UserPatientDb> captor = ArgumentCaptor.forClass(UserPatientDb.class);
        verify(repository).save(captor.capture());

        assertEquals(AccessLevel.OWNER, captor.getValue().getAccessLevel());
        // A null field on the update leaves the stored value alone.
        assertEquals(USER_ID, captor.getValue().getUserId());
        assertNotNull(captor.getValue().getUpdatedAt());
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        String extid = UUID.randomUUID().toString();
        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        assertThrows(ServiceException.class,
                () -> service.update(extid, DomainBuilderDatabase.getUserPatient()));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        String extid = UUID.randomUUID().toString();
        UserPatientDb existing = DomainBuilderDatabase.getUserPatientDb(
                USER_ID, PATIENT_ID, AccessLevel.VIEW_RECORD, extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existing));
        when(repository.save(any(UserPatientDb.class))).thenReturn(existing);

        assertTrue(service.delete(extid));

        ArgumentCaptor<UserPatientDb> captor = ArgumentCaptor.forClass(UserPatientDb.class);
        verify(repository).save(captor.capture());

        assertNotNull(captor.getValue().getDeletedAt());
        assertEquals(ActiveEnum.INACTIVE, captor.getValue().getActive());
    }

    @Test
    void findByExtid_shouldThrowException_whenNotFound() {
        String extid = UUID.randomUUID().toString();
        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        assertThrows(ServiceException.class, () -> service.findByExtid(extid));
    }

    @Test
    void findActiveGrant_shouldReturnEmptyOptional_ratherThanThrowing_whenNoGrant() {
        when(repository.findActiveGrant(USER_ID, PATIENT_ID)).thenReturn(Optional.empty());

        assertTrue(service.findActiveGrant(USER_ID, PATIENT_ID).isEmpty());
    }

    @Test
    void findActiveGrantsForUser_shouldReturnGrants() {
        List<UserPatientDb> records = List.of(
                DomainBuilderDatabase.getUserPatientDb(USER_ID, PATIENT_ID),
                DomainBuilderDatabase.getUserPatientDb(USER_ID, PATIENT_ID + 1));

        when(repository.findActiveGrantsForUser(USER_ID)).thenReturn(records);
        when(mapper.toModelList(records)).thenReturn(List.of(
                DomainBuilderDatabase.getUserPatient(),
                DomainBuilderDatabase.getUserPatient()));

        assertEquals(2, service.findActiveGrantsForUser(USER_ID).size());
    }

    @Test
    void findAll_shouldReturnAllEntities() {
        List<UserPatientDb> records = List.of(DomainBuilderDatabase.getUserPatientDb());

        when(repository.findAllActive()).thenReturn(records);
        when(mapper.toModelList(records)).thenReturn(List.of(DomainBuilderDatabase.getUserPatient()));

        assertEquals(1, service.findAll().size());
    }
}
