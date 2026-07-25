package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.TrialStatus;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.entity.TrialStatusDb;
import com.seibel.cancer.database.db.mapper.TrialStatusMapper;
import com.seibel.cancer.database.db.repository.TrialStatusRepository;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrialStatusDbServiceTest {

    @Mock
    private TrialStatusRepository repository;

    @Mock
    private TrialStatusMapper mapper;

    @InjectMocks
    private TrialStatusDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        TrialStatus input = DomainBuilderDatabase.getTrialStatus();
        TrialStatusDb savedDb = DomainBuilderDatabase.getTrialStatusDb(input.getTrialId(), input.getAppUserId());
        TrialStatus expectedDomain = DomainBuilderDatabase.getTrialStatus(savedDb);

        when(mapper.toDb(input)).thenReturn(new TrialStatusDb());
        when(repository.save(any(TrialStatusDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        TrialStatus result = service.create(input);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<TrialStatusDb> captor = ArgumentCaptor.forClass(TrialStatusDb.class);
        verify(repository).save(captor.capture());

        TrialStatusDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        // Arrange
        TrialStatus input = DomainBuilderDatabase.getTrialStatus();
        when(mapper.toDb(input)).thenReturn(new TrialStatusDb());
        when(repository.save(any(TrialStatusDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.create(input));
        verify(repository).save(any(TrialStatusDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void create_positional_shouldGenerateUuidAndSetFields() {
        // Arrange
        Long trialId = 10L;
        Long appUserId = 20L;
        String status = "ENROLLED";
        String notes = "Some notes";
        LocalDateTime statusChangedAt = LocalDateTime.now();

        TrialStatusDb savedDb = DomainBuilderDatabase.getTrialStatusDb(trialId, appUserId, status, null);
        TrialStatus expectedDomain = DomainBuilderDatabase.getTrialStatus(savedDb);

        when(repository.save(any(TrialStatusDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        TrialStatus result = service.create(trialId, appUserId, status, notes, statusChangedAt);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<TrialStatusDb> captor = ArgumentCaptor.forClass(TrialStatusDb.class);
        verify(repository).save(captor.capture());

        TrialStatusDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(trialId, captured.getTrialId());
        assertEquals(appUserId, captured.getAppUserId());
        assertEquals(status, captured.getStatus());
        assertEquals(notes, captured.getNotes());
        assertEquals(statusChangedAt, captured.getStatusChangedAt());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void create_positional_shouldThrowException_whenRepositoryFails() {
        // Arrange
        when(repository.save(any(TrialStatusDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () ->
                service.create(1L, 2L, "STATUS", "notes", LocalDateTime.now()));
        verify(repository).save(any(TrialStatusDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        TrialStatusDb existingDb = DomainBuilderDatabase.getTrialStatusDb(1L, 2L, "OLD_STATUS", extid);

        TrialStatusDb updatedDb = DomainBuilderDatabase.getTrialStatusDb(1L, 2L, "NEW_STATUS", extid);
        TrialStatus expectedDomain = DomainBuilderDatabase.getTrialStatus(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(TrialStatusDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        TrialStatus result = service.update(extid, null, null, "NEW_STATUS", null, null);

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<TrialStatusDb> captor = ArgumentCaptor.forClass(TrialStatusDb.class);
        verify(repository).save(captor.capture());

        TrialStatusDb captured = captor.getValue();
        assertEquals("NEW_STATUS", captured.getStatus());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(updatedDb);
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        // Arrange
        String extid = "nonexistent-extid";
        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ServiceException.class, () ->
                service.update(extid, null, null, "NEW_STATUS", null, null));
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        // Arrange
        String extid = "existing-extid";
        TrialStatusDb existingDb = DomainBuilderDatabase.getTrialStatusDb(1L, 2L, "OLD_STATUS", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(TrialStatusDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () ->
                service.update(extid, null, null, "NEW_STATUS", null, null));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        TrialStatusDb existingDb = DomainBuilderDatabase.getTrialStatusDb(1L, 2L, "STATUS", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(TrialStatusDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<TrialStatusDb> captor = ArgumentCaptor.forClass(TrialStatusDb.class);
        verify(repository).save(captor.capture());

        TrialStatusDb captured = captor.getValue();
        assertNotNull(captured.getDeletedAt());
        assertEquals(ActiveEnum.INACTIVE, captured.getActive());
    }

    @Test
    void delete_shouldThrowException_whenNotFound() {
        // Arrange
        String extid = "nonexistent-extid";
        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.delete(extid));
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
    }

    @Test
    void findByExtid_shouldReturnTrialStatus_whenExists() {
        // Arrange
        String extid = "existing-extid";
        TrialStatusDb db = DomainBuilderDatabase.getTrialStatusDb(1L, 2L, "STATUS", extid);
        TrialStatus expectedDomain = DomainBuilderDatabase.getTrialStatus(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        TrialStatus result = service.findByExtid(extid);

        // Assert
        assertNotNull(result);
        assertEquals(expectedDomain.getExtid(), result.getExtid());
        verify(repository).findByExtid(extid);
        verify(mapper).toModel(db);
    }

    @Test
    void findByExtid_shouldThrowException_whenNotFound() {
        // Arrange
        String extid = "nonexistent-extid";
        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.findByExtid(extid));
        verify(repository).findByExtid(extid);
        verify(mapper, never()).toModel(any());
    }

    @Test
    void findAll_shouldReturnAllEntities() {
        // Arrange
        TrialStatusDb db1 = DomainBuilderDatabase.getTrialStatusDb();
        TrialStatusDb db2 = DomainBuilderDatabase.getTrialStatusDb();
        List<TrialStatusDb> dbList = Arrays.asList(db1, db2);

        TrialStatus domain1 = DomainBuilderDatabase.getTrialStatus(db1);
        TrialStatus domain2 = DomainBuilderDatabase.getTrialStatus(db2);
        List<TrialStatus> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<TrialStatus> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredEntities() {
        // Arrange
        TrialStatusDb db1 = DomainBuilderDatabase.getTrialStatusDb();
        db1.setActive(ActiveEnum.ACTIVE);
        TrialStatusDb db2 = DomainBuilderDatabase.getTrialStatusDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<TrialStatusDb> dbList = Arrays.asList(db1, db2);

        TrialStatus domain1 = DomainBuilderDatabase.getTrialStatus(db1);
        TrialStatus domain2 = DomainBuilderDatabase.getTrialStatus(db2);
        List<TrialStatus> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(any(ActiveEnum.class))).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<TrialStatus> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByActive(any(ActiveEnum.class));
        verify(mapper).toModelList(dbList);
    }
}
