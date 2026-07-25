package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.TrialSource;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.TrialSourceDb;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.mapper.TrialSourceMapper;
import com.seibel.cancer.database.db.repository.TrialSourceRepository;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrialSourceDbServiceTest {

    @Mock
    private TrialSourceRepository repository;

    @Mock
    private TrialSourceMapper mapper;

    @InjectMocks
    private TrialSourceDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        TrialSource input = DomainBuilderDatabase.getTrialSource();
        TrialSourceDb savedDb = DomainBuilderDatabase.getTrialSourceDb(input.getCode(), input.getName());
        TrialSource expectedDomain = DomainBuilderDatabase.getTrialSource(savedDb);

        when(mapper.toDb(input)).thenReturn(new TrialSourceDb());
        when(repository.save(any(TrialSourceDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        TrialSource result = service.create(input);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<TrialSourceDb> captor = ArgumentCaptor.forClass(TrialSourceDb.class);
        verify(repository).save(captor.capture());

        TrialSourceDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        // Arrange
        TrialSource input = DomainBuilderDatabase.getTrialSource();
        when(mapper.toDb(input)).thenReturn(new TrialSourceDb());
        when(repository.save(any(TrialSourceDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.create(input));
        verify(repository).save(any(TrialSourceDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void create_positional_shouldGenerateUuidAndSetFields() {
        // Arrange
        String code = "CTGOV";
        String name = "ClinicalTrials.gov";
        String baseUrl = "https://clinicaltrials.gov";

        TrialSourceDb savedDb = DomainBuilderDatabase.getTrialSourceDb(code, name, baseUrl, null);
        TrialSource expectedDomain = DomainBuilderDatabase.getTrialSource(savedDb);

        when(repository.save(any(TrialSourceDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        TrialSource result = service.create(code, name, baseUrl);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<TrialSourceDb> captor = ArgumentCaptor.forClass(TrialSourceDb.class);
        verify(repository).save(captor.capture());

        TrialSourceDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(code, captured.getCode());
        assertEquals(name, captured.getName());
        assertEquals(baseUrl, captured.getBaseUrl());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void create_positional_shouldThrowException_whenRepositoryFails() {
        // Arrange
        when(repository.save(any(TrialSourceDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.create("CODE", "Name", "https://example.com"));
        verify(repository).save(any(TrialSourceDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        TrialSourceDb existingDb = DomainBuilderDatabase.getTrialSourceDb("TS_OLD", "Old Name", "https://old.example.com", extid);

        TrialSourceDb updatedDb = DomainBuilderDatabase.getTrialSourceDb("TS_OLD", "Updated Name", "https://new.example.com", extid);
        TrialSource expectedDomain = DomainBuilderDatabase.getTrialSource(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(TrialSourceDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        TrialSource result = service.update(extid, null, "Updated Name", "https://new.example.com");

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<TrialSourceDb> captor = ArgumentCaptor.forClass(TrialSourceDb.class);
        verify(repository).save(captor.capture());

        TrialSourceDb captured = captor.getValue();
        assertEquals("Updated Name", captured.getName());
        assertEquals("https://new.example.com", captured.getBaseUrl());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(updatedDb);
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        // Arrange
        String extid = "nonexistent-extid";
        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.update(extid, null, "Doesn't matter", null));
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        // Arrange
        String extid = "existing-extid";
        TrialSourceDb existingDb = DomainBuilderDatabase.getTrialSourceDb("TS_X", "Name", "https://example.com", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(TrialSourceDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.update(extid, null, "New Name", null));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        TrialSourceDb existingDb = DomainBuilderDatabase.getTrialSourceDb("TS_X", "Name", "https://example.com", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(TrialSourceDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<TrialSourceDb> captor = ArgumentCaptor.forClass(TrialSourceDb.class);
        verify(repository).save(captor.capture());

        TrialSourceDb captured = captor.getValue();
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
    void findByExtid_shouldReturnEntity_whenExists() {
        // Arrange
        String extid = "existing-extid";
        TrialSourceDb db = DomainBuilderDatabase.getTrialSourceDb("TS_X", "Name", "https://example.com", extid);
        TrialSource expectedDomain = DomainBuilderDatabase.getTrialSource(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        TrialSource result = service.findByExtid(extid);

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
        TrialSourceDb db1 = DomainBuilderDatabase.getTrialSourceDb();
        TrialSourceDb db2 = DomainBuilderDatabase.getTrialSourceDb();
        List<TrialSourceDb> dbList = Arrays.asList(db1, db2);

        TrialSource domain1 = DomainBuilderDatabase.getTrialSource(db1);
        TrialSource domain2 = DomainBuilderDatabase.getTrialSource(db2);
        List<TrialSource> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<TrialSource> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredEntities() {
        // Arrange
        TrialSourceDb db1 = DomainBuilderDatabase.getTrialSourceDb();
        db1.setActive(ActiveEnum.ACTIVE);
        TrialSourceDb db2 = DomainBuilderDatabase.getTrialSourceDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<TrialSourceDb> dbList = Arrays.asList(db1, db2);

        TrialSource domain1 = DomainBuilderDatabase.getTrialSource(db1);
        TrialSource domain2 = DomainBuilderDatabase.getTrialSource(db2);
        List<TrialSource> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(any(ActiveEnum.class))).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<TrialSource> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByActive(any(ActiveEnum.class));
        verify(mapper).toModelList(dbList);
    }
}
