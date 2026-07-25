package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.Intervention;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.InterventionDb;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.mapper.InterventionMapper;
import com.seibel.cancer.database.db.repository.InterventionRepository;
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
class InterventionDbServiceTest {

    @Mock
    private InterventionRepository repository;

    @Mock
    private InterventionMapper mapper;

    @InjectMocks
    private InterventionDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        Intervention input = DomainBuilderDatabase.getIntervention();
        InterventionDb savedDb = DomainBuilderDatabase.getInterventionDb(input.getType(), input.getName());
        Intervention expectedDomain = DomainBuilderDatabase.getIntervention(savedDb);

        when(mapper.toDb(input)).thenReturn(new InterventionDb());
        when(repository.save(any(InterventionDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        Intervention result = service.create(input);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<InterventionDb> captor = ArgumentCaptor.forClass(InterventionDb.class);
        verify(repository).save(captor.capture());

        InterventionDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        // Arrange
        Intervention input = DomainBuilderDatabase.getIntervention();
        when(mapper.toDb(input)).thenReturn(new InterventionDb());
        when(repository.save(any(InterventionDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.create(input));
        verify(repository).save(any(InterventionDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        InterventionDb existingDb = DomainBuilderDatabase.getInterventionDb("DRUG", "Old Name", 100L, extid);

        InterventionDb updatedDb = DomainBuilderDatabase.getInterventionDb("PROCEDURE", "Updated Name", 200L, extid);
        Intervention expectedDomain = DomainBuilderDatabase.getIntervention(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(InterventionDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        Intervention result = service.update(extid, 200L, "PROCEDURE", "Updated Name", null);

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<InterventionDb> captor = ArgumentCaptor.forClass(InterventionDb.class);
        verify(repository).save(captor.capture());

        InterventionDb captured = captor.getValue();
        assertEquals(200L, captured.getTrialId());
        assertEquals("PROCEDURE", captured.getType());
        assertEquals("Updated Name", captured.getName());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(updatedDb);
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        // Arrange
        String extid = "nonexistent-extid";
        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.update(extid, 1L, "DRUG", "Doesn't matter", null));
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        // Arrange
        String extid = "existing-extid";
        InterventionDb existingDb = DomainBuilderDatabase.getInterventionDb("DRUG", "Name", 100L, extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(InterventionDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.update(extid, 100L, "DRUG", "New Name", null));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        InterventionDb existingDb = DomainBuilderDatabase.getInterventionDb("DRUG", "Name", 100L, extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(InterventionDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<InterventionDb> captor = ArgumentCaptor.forClass(InterventionDb.class);
        verify(repository).save(captor.capture());

        InterventionDb captured = captor.getValue();
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
    void findByExtid_shouldReturnIntervention_whenExists() {
        // Arrange
        String extid = "existing-extid";
        InterventionDb db = DomainBuilderDatabase.getInterventionDb("DRUG", "Name", 100L, extid);
        Intervention expectedDomain = DomainBuilderDatabase.getIntervention(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        Intervention result = service.findByExtid(extid);

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
        InterventionDb db1 = DomainBuilderDatabase.getInterventionDb();
        InterventionDb db2 = DomainBuilderDatabase.getInterventionDb();
        List<InterventionDb> dbList = Arrays.asList(db1, db2);

        Intervention domain1 = DomainBuilderDatabase.getIntervention(db1);
        Intervention domain2 = DomainBuilderDatabase.getIntervention(db2);
        List<Intervention> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Intervention> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredEntities() {
        // Arrange
        InterventionDb db1 = DomainBuilderDatabase.getInterventionDb();
        db1.setActive(ActiveEnum.ACTIVE);
        InterventionDb db2 = DomainBuilderDatabase.getInterventionDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<InterventionDb> dbList = Arrays.asList(db1, db2);

        Intervention domain1 = DomainBuilderDatabase.getIntervention(db1);
        Intervention domain2 = DomainBuilderDatabase.getIntervention(db2);
        List<Intervention> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(any(ActiveEnum.class))).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Intervention> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByActive(any(ActiveEnum.class));
        verify(mapper).toModelList(dbList);
    }
}
