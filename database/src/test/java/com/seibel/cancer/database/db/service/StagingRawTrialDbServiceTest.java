package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.StagingRawTrial;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.StagingRawTrialDb;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.mapper.StagingRawTrialMapper;
import com.seibel.cancer.database.db.repository.StagingRawTrialRepository;
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
class StagingRawTrialDbServiceTest {

    @Mock
    private StagingRawTrialRepository repository;

    @Mock
    private StagingRawTrialMapper mapper;

    @InjectMocks
    private StagingRawTrialDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        StagingRawTrial input = DomainBuilderDatabase.getStagingRawTrial();
        StagingRawTrialDb savedDb = DomainBuilderDatabase.getStagingRawTrialDb(input.getTrialSourceId(), input.getSourceTrialId());
        StagingRawTrial expectedDomain = DomainBuilderDatabase.getStagingRawTrial(savedDb);

        when(mapper.toDb(input)).thenReturn(new StagingRawTrialDb());
        when(repository.save(any(StagingRawTrialDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        StagingRawTrial result = service.create(input);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<StagingRawTrialDb> captor = ArgumentCaptor.forClass(StagingRawTrialDb.class);
        verify(repository).save(captor.capture());

        StagingRawTrialDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void create_shouldPersistPayloadHash() {
        // Arrange
        StagingRawTrialDb savedDb = DomainBuilderDatabase.getStagingRawTrialDb();
        when(repository.save(any(StagingRawTrialDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(DomainBuilderDatabase.getStagingRawTrial(savedDb));

        // Act
        service.create(1L, "NCT00000001", "{\"a\":1}", "abc123", LocalDateTime.now(), null, null);

        // Assert
        ArgumentCaptor<StagingRawTrialDb> captor = ArgumentCaptor.forClass(StagingRawTrialDb.class);
        verify(repository).save(captor.capture());
        assertEquals("abc123", captor.getValue().getPayloadHash());
        assertEquals("{\"a\":1}", captor.getValue().getRawPayload());
    }

    /**
     * The characteristic bug this guards: a refresh that replaces the payload but leaves the
     * old hash would make the next pull compare against a stale value and skip a trial that
     * had in fact changed. Payload and hash must always be written together.
     */
    @Test
    void refreshForRenormalization_shouldUpdateHashAlongsidePayload() {
        // Arrange
        StagingRawTrialDb existing = DomainBuilderDatabase.getStagingRawTrialDb();
        existing.setPayloadHash("oldhash");
        existing.setRawPayload("{\"old\":true}");
        existing.setNormalizedAt(LocalDateTime.now());

        when(repository.findByExtid(existing.getExtid())).thenReturn(Optional.of(existing));
        when(repository.save(any(StagingRawTrialDb.class))).thenReturn(existing);
        when(mapper.toModel(existing)).thenReturn(DomainBuilderDatabase.getStagingRawTrial(existing));

        // Act
        service.refreshForRenormalization(existing.getExtid(), "{\"new\":true}", "newhash",
                LocalDateTime.now());

        // Assert
        ArgumentCaptor<StagingRawTrialDb> captor = ArgumentCaptor.forClass(StagingRawTrialDb.class);
        verify(repository).save(captor.capture());

        StagingRawTrialDb captured = captor.getValue();
        assertEquals("newhash", captured.getPayloadHash());
        assertEquals("{\"new\":true}", captured.getRawPayload());
        // Cleared so the row re-enters the pending queue.
        assertNull(captured.getNormalizedAt());
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        // Arrange
        StagingRawTrial input = DomainBuilderDatabase.getStagingRawTrial();
        when(mapper.toDb(input)).thenReturn(new StagingRawTrialDb());
        when(repository.save(any(StagingRawTrialDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.create(input));
        verify(repository).save(any(StagingRawTrialDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void create_positional_shouldGenerateUuidAndSetFields() {
        // Arrange
        Long trialSourceId = 42L;
        String sourceTrialId = "SRC_ABCDEF";
        String rawPayload = "{\"foo\":\"bar\"}";
        LocalDateTime fetchedAt = LocalDateTime.now();

        StagingRawTrialDb savedDb = DomainBuilderDatabase.getStagingRawTrialDb(trialSourceId, sourceTrialId);
        StagingRawTrial expectedDomain = DomainBuilderDatabase.getStagingRawTrial(savedDb);

        when(repository.save(any(StagingRawTrialDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        StagingRawTrial result = service.create(trialSourceId, sourceTrialId, rawPayload, fetchedAt, null, null);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<StagingRawTrialDb> captor = ArgumentCaptor.forClass(StagingRawTrialDb.class);
        verify(repository).save(captor.capture());

        StagingRawTrialDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(trialSourceId, captured.getTrialSourceId());
        assertEquals(sourceTrialId, captured.getSourceTrialId());
        assertEquals(rawPayload, captured.getRawPayload());
        assertEquals(fetchedAt, captured.getFetchedAt());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void create_positional_shouldThrowException_whenRepositoryFails() {
        // Arrange
        Long trialSourceId = 42L;
        String sourceTrialId = "SRC_ABCDEF";
        LocalDateTime fetchedAt = LocalDateTime.now();
        when(repository.save(any(StagingRawTrialDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class,
                () -> service.create(trialSourceId, sourceTrialId, null, fetchedAt, null, null));
        verify(repository).save(any(StagingRawTrialDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        StagingRawTrialDb existingDb = DomainBuilderDatabase.getStagingRawTrialDb(1L, "SRC_OLD", null, extid);

        String newSourceTrialId = "SRC_NEW";
        String newNormalizationError = "Parse failure";

        StagingRawTrialDb updatedDb = DomainBuilderDatabase.getStagingRawTrialDb(1L, newSourceTrialId, newNormalizationError, extid);
        StagingRawTrial expectedDomain = DomainBuilderDatabase.getStagingRawTrial(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(StagingRawTrialDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        StagingRawTrial result = service.update(extid, null, newSourceTrialId, null, null, null, newNormalizationError);

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<StagingRawTrialDb> captor = ArgumentCaptor.forClass(StagingRawTrialDb.class);
        verify(repository).save(captor.capture());

        StagingRawTrialDb captured = captor.getValue();
        assertEquals(newSourceTrialId, captured.getSourceTrialId());
        assertEquals(newNormalizationError, captured.getNormalizationError());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(updatedDb);
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        // Arrange
        String extid = "nonexistent-extid";
        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ServiceException.class,
                () -> service.update(extid, null, "Doesn't matter", null, null, null, null));
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        // Arrange
        String extid = "existing-extid";
        StagingRawTrialDb existingDb = DomainBuilderDatabase.getStagingRawTrialDb(1L, "SRC_X", null, extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(StagingRawTrialDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class,
                () -> service.update(extid, null, "New Source Id", null, null, null, null));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        StagingRawTrialDb existingDb = DomainBuilderDatabase.getStagingRawTrialDb(1L, "SRC_X", null, extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(StagingRawTrialDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<StagingRawTrialDb> captor = ArgumentCaptor.forClass(StagingRawTrialDb.class);
        verify(repository).save(captor.capture());

        StagingRawTrialDb captured = captor.getValue();
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
    void findByExtid_shouldReturnStagingRawTrial_whenExists() {
        // Arrange
        String extid = "existing-extid";
        StagingRawTrialDb db = DomainBuilderDatabase.getStagingRawTrialDb(1L, "SRC_X", null, extid);
        StagingRawTrial expectedDomain = DomainBuilderDatabase.getStagingRawTrial(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        StagingRawTrial result = service.findByExtid(extid);

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
        StagingRawTrialDb db1 = DomainBuilderDatabase.getStagingRawTrialDb();
        StagingRawTrialDb db2 = DomainBuilderDatabase.getStagingRawTrialDb();
        List<StagingRawTrialDb> dbList = Arrays.asList(db1, db2);

        StagingRawTrial domain1 = DomainBuilderDatabase.getStagingRawTrial(db1);
        StagingRawTrial domain2 = DomainBuilderDatabase.getStagingRawTrial(db2);
        List<StagingRawTrial> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<StagingRawTrial> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredEntities() {
        // Arrange
        StagingRawTrialDb db1 = DomainBuilderDatabase.getStagingRawTrialDb();
        db1.setActive(ActiveEnum.ACTIVE);
        StagingRawTrialDb db2 = DomainBuilderDatabase.getStagingRawTrialDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<StagingRawTrialDb> dbList = Arrays.asList(db1, db2);

        StagingRawTrial domain1 = DomainBuilderDatabase.getStagingRawTrial(db1);
        StagingRawTrial domain2 = DomainBuilderDatabase.getStagingRawTrial(db2);
        List<StagingRawTrial> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(any(ActiveEnum.class))).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<StagingRawTrial> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByActive(any(ActiveEnum.class));
        verify(mapper).toModelList(dbList);
    }
}
