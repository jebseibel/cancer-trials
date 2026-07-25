package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.TrialDb;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.mapper.TrialMapper;
import com.seibel.cancer.database.db.repository.TrialRepository;
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
class TrialDbServiceTest {

    @Mock
    private TrialRepository repository;

    @Mock
    private TrialMapper mapper;

    @InjectMocks
    private TrialDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        Trial input = DomainBuilderDatabase.getTrial();
        TrialDb savedDb = DomainBuilderDatabase.getTrialDb(input.getNctId(), input.getBriefTitle());
        Trial expectedDomain = DomainBuilderDatabase.getTrial(savedDb);

        when(mapper.toDb(input)).thenReturn(new TrialDb());
        when(repository.save(any(TrialDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        Trial result = service.create(input);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<TrialDb> captor = ArgumentCaptor.forClass(TrialDb.class);
        verify(repository).save(captor.capture());

        TrialDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        // Arrange
        Trial input = DomainBuilderDatabase.getTrial();
        when(mapper.toDb(input)).thenReturn(new TrialDb());
        when(repository.save(any(TrialDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.create(input));
        verify(repository).save(any(TrialDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        TrialDb existingDb = DomainBuilderDatabase.getTrialDb("NCT_OLD", "Old Title", null, extid);

        Trial changes = Trial.builder()
                .briefTitle("Updated Title")
                .overallStatus("RECRUITING")
                .build();

        TrialDb updatedDb = DomainBuilderDatabase.getTrialDb("NCT_OLD", "Updated Title", "RECRUITING", extid);
        Trial expectedDomain = DomainBuilderDatabase.getTrial(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(TrialDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        Trial result = service.update(extid, changes);

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<TrialDb> captor = ArgumentCaptor.forClass(TrialDb.class);
        verify(repository).save(captor.capture());

        TrialDb captured = captor.getValue();
        assertEquals("Updated Title", captured.getBriefTitle());
        assertEquals("RECRUITING", captured.getOverallStatus());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(updatedDb);
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        // Arrange
        String extid = "nonexistent-extid";
        Trial changes = Trial.builder().briefTitle("Doesn't matter").build();
        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.update(extid, changes));
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        // Arrange
        String extid = "existing-extid";
        TrialDb existingDb = DomainBuilderDatabase.getTrialDb("NCT_X", "Title", null, extid);
        Trial changes = Trial.builder().briefTitle("New Title").build();

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(TrialDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.update(extid, changes));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        TrialDb existingDb = DomainBuilderDatabase.getTrialDb("NCT_X", "Title", null, extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(TrialDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<TrialDb> captor = ArgumentCaptor.forClass(TrialDb.class);
        verify(repository).save(captor.capture());

        TrialDb captured = captor.getValue();
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
    void findByExtid_shouldReturnTrial_whenExists() {
        // Arrange
        String extid = "existing-extid";
        TrialDb db = DomainBuilderDatabase.getTrialDb("NCT_X", "Title", null, extid);
        Trial expectedDomain = DomainBuilderDatabase.getTrial(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        Trial result = service.findByExtid(extid);

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
    void findAll_shouldReturnAllTrials() {
        // Arrange
        TrialDb db1 = DomainBuilderDatabase.getTrialDb();
        TrialDb db2 = DomainBuilderDatabase.getTrialDb();
        List<TrialDb> dbList = Arrays.asList(db1, db2);

        Trial domain1 = DomainBuilderDatabase.getTrial(db1);
        Trial domain2 = DomainBuilderDatabase.getTrial(db2);
        List<Trial> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Trial> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredTrials() {
        // Arrange
        TrialDb db1 = DomainBuilderDatabase.getTrialDb();
        db1.setActive(ActiveEnum.ACTIVE);
        TrialDb db2 = DomainBuilderDatabase.getTrialDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<TrialDb> dbList = Arrays.asList(db1, db2);

        Trial domain1 = DomainBuilderDatabase.getTrial(db1);
        Trial domain2 = DomainBuilderDatabase.getTrial(db2);
        List<Trial> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(any(ActiveEnum.class))).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Trial> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByActive(any(ActiveEnum.class));
        verify(mapper).toModelList(dbList);
    }
}
