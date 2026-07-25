package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.ArmGroup;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.ArmGroupDb;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.mapper.ArmGroupMapper;
import com.seibel.cancer.database.db.repository.ArmGroupRepository;
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
class ArmGroupDbServiceTest {

    @Mock
    private ArmGroupRepository repository;

    @Mock
    private ArmGroupMapper mapper;

    @InjectMocks
    private ArmGroupDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        Long trialId = 1L;
        String label = "Arm A";
        String type = "EXPERIMENTAL";
        String description = "Test Description";
        ArmGroupDb savedDb = DomainBuilderDatabase.getArmGroupDb(trialId, label);
        ArmGroup expectedDomain = DomainBuilderDatabase.getArmGroup(savedDb);

        when(repository.save(any(ArmGroupDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        ArmGroup result = service.create(trialId, label, type, description);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<ArmGroupDb> captor = ArgumentCaptor.forClass(ArmGroupDb.class);
        verify(repository).save(captor.capture());

        ArmGroupDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(trialId, captured.getTrialId());
        assertEquals(label, captured.getLabel());
        assertEquals(type, captured.getType());
        assertEquals(description, captured.getDescription());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        // Arrange
        when(repository.save(any(ArmGroupDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> {
            service.create(1L, "Arm A", "EXPERIMENTAL", "Description");
        });
        verify(repository).save(any(ArmGroupDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        Long trialId = 2L;
        String label = "Updated Arm";
        String type = "PLACEBO_COMPARATOR";
        String description = "Updated Description";

        ArmGroupDb existingDb = DomainBuilderDatabase.getArmGroupDb(1L, "Old Arm", "EXPERIMENTAL", "Old Description", extid);
        ArmGroupDb updatedDb = DomainBuilderDatabase.getArmGroupDb(trialId, label, type, description, extid);
        ArmGroup expectedDomain = DomainBuilderDatabase.getArmGroup(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(ArmGroupDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        ArmGroup result = service.update(extid, trialId, label, type, description);

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<ArmGroupDb> captor = ArgumentCaptor.forClass(ArmGroupDb.class);
        verify(repository).save(captor.capture());

        ArmGroupDb captured = captor.getValue();
        assertEquals(trialId, captured.getTrialId());
        assertEquals(label, captured.getLabel());
        assertEquals(type, captured.getType());
        assertEquals(description, captured.getDescription());
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
            service.update(extid, 1L, "Arm A", "EXPERIMENTAL", "Description");
        });
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        // Arrange
        String extid = "existing-extid";
        ArmGroupDb existingDb = DomainBuilderDatabase.getArmGroupDb(1L, "Arm A", "EXPERIMENTAL", "Description", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(ArmGroupDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> {
            service.update(extid, 2L, "New Arm", "PLACEBO_COMPARATOR", "New Description");
        });
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        ArmGroupDb existingDb = DomainBuilderDatabase.getArmGroupDb(1L, "Arm A", "EXPERIMENTAL", "Description", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(ArmGroupDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<ArmGroupDb> captor = ArgumentCaptor.forClass(ArmGroupDb.class);
        verify(repository).save(captor.capture());

        ArmGroupDb captured = captor.getValue();
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
        ArmGroupDb db = DomainBuilderDatabase.getArmGroupDb(1L, "Arm A", "EXPERIMENTAL", "Description", extid);
        ArmGroup expectedDomain = DomainBuilderDatabase.getArmGroup(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        ArmGroup result = service.findByExtid(extid);

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
        ArmGroupDb db1 = DomainBuilderDatabase.getArmGroupDb();
        ArmGroupDb db2 = DomainBuilderDatabase.getArmGroupDb();
        List<ArmGroupDb> dbList = Arrays.asList(db1, db2);

        ArmGroup domain1 = DomainBuilderDatabase.getArmGroup(db1);
        ArmGroup domain2 = DomainBuilderDatabase.getArmGroup(db2);
        List<ArmGroup> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<ArmGroup> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredEntities() {
        // Arrange
        ArmGroupDb db1 = DomainBuilderDatabase.getArmGroupDb();
        db1.setActive(ActiveEnum.ACTIVE);
        ArmGroupDb db2 = DomainBuilderDatabase.getArmGroupDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<ArmGroupDb> dbList = Arrays.asList(db1, db2);

        ArmGroup domain1 = DomainBuilderDatabase.getArmGroup(db1);
        ArmGroup domain2 = DomainBuilderDatabase.getArmGroup(db2);
        List<ArmGroup> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(ActiveEnum.ACTIVE)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<ArmGroup> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByActive(ActiveEnum.ACTIVE);
        verify(mapper).toModelList(dbList);
    }
}
