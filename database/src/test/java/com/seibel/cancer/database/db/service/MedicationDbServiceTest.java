package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.Medication;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.MedicationDb;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.mapper.MedicationMapper;
import com.seibel.cancer.database.db.repository.MedicationRepository;
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
class MedicationDbServiceTest {

    @Mock
    private MedicationRepository repository;

    @Mock
    private MedicationMapper mapper;

    @InjectMocks
    private MedicationDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        String name = "Test Medication";
        MedicationDb savedDb = DomainBuilderDatabase.getMedicationDb(name, null);
        Medication expectedDomain = DomainBuilderDatabase.getMedication(savedDb);

        when(repository.save(any(MedicationDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        Medication result = service.create(name);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<MedicationDb> captor = ArgumentCaptor.forClass(MedicationDb.class);
        verify(repository).save(captor.capture());

        MedicationDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(name, captured.getName());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        // Arrange
        when(repository.save(any(MedicationDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> {
            service.create("Some Medication");
        });
        verify(repository).save(any(MedicationDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        String name = "Updated Medication";

        MedicationDb existingDb = DomainBuilderDatabase.getMedicationDb("Old Medication", extid);
        MedicationDb updatedDb = DomainBuilderDatabase.getMedicationDb(name, extid);
        Medication expectedDomain = DomainBuilderDatabase.getMedication(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(MedicationDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        Medication result = service.update(extid, name);

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<MedicationDb> captor = ArgumentCaptor.forClass(MedicationDb.class);
        verify(repository).save(captor.capture());

        MedicationDb captured = captor.getValue();
        assertEquals(name, captured.getName());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(updatedDb);
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        // Arrange
        String extid = "nonexistent-extid";
        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ServiceException.class, () -> {
            service.update(extid, "New Name");
        });
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        // Arrange
        String extid = "existing-extid";
        MedicationDb existingDb = DomainBuilderDatabase.getMedicationDb("Name", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(MedicationDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> {
            service.update(extid, "New Name");
        });
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        MedicationDb existingDb = DomainBuilderDatabase.getMedicationDb("Name", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(MedicationDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<MedicationDb> captor = ArgumentCaptor.forClass(MedicationDb.class);
        verify(repository).save(captor.capture());

        MedicationDb captured = captor.getValue();
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
    void findByExtid_shouldReturnMedication_whenExists() {
        // Arrange
        String extid = "existing-extid";
        MedicationDb db = DomainBuilderDatabase.getMedicationDb("Name", extid);
        Medication expectedDomain = DomainBuilderDatabase.getMedication(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        Medication result = service.findByExtid(extid);

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
        assertThrows(ServiceException.class, () -> {
            service.findByExtid(extid);
        });
        verify(repository).findByExtid(extid);
        verify(mapper, never()).toModel(any());
    }

    @Test
    void findAll_shouldReturnAllEntities() {
        // Arrange
        MedicationDb db1 = DomainBuilderDatabase.getMedicationDb();
        MedicationDb db2 = DomainBuilderDatabase.getMedicationDb();
        List<MedicationDb> dbList = Arrays.asList(db1, db2);

        Medication domain1 = DomainBuilderDatabase.getMedication(db1);
        Medication domain2 = DomainBuilderDatabase.getMedication(db2);
        List<Medication> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Medication> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredEntities() {
        // Arrange
        MedicationDb db1 = DomainBuilderDatabase.getMedicationDb();
        db1.setActive(ActiveEnum.ACTIVE);
        MedicationDb db2 = DomainBuilderDatabase.getMedicationDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<MedicationDb> dbList = Arrays.asList(db1, db2);

        Medication domain1 = DomainBuilderDatabase.getMedication(db1);
        Medication domain2 = DomainBuilderDatabase.getMedication(db2);
        List<Medication> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(ActiveEnum.ACTIVE)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Medication> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByActive(ActiveEnum.ACTIVE);
        verify(mapper).toModelList(dbList);
    }
}
