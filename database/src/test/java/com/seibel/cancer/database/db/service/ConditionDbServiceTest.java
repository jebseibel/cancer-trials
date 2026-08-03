package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.Condition;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.MedicalConditionDb;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.mapper.ConditionMapper;
import com.seibel.cancer.database.db.repository.ConditionRepository;
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
class ConditionDbServiceTest {

    @Mock
    private ConditionRepository repository;

    @Mock
    private ConditionMapper mapper;

    @InjectMocks
    private ConditionDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        String name = "Test Condition";
        MedicalConditionDb savedDb = DomainBuilderDatabase.getConditionDb(name, null);
        Condition expectedDomain = DomainBuilderDatabase.getCondition(savedDb);

        when(repository.save(any(MedicalConditionDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        Condition result = service.create(name);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<MedicalConditionDb> captor = ArgumentCaptor.forClass(MedicalConditionDb.class);
        verify(repository).save(captor.capture());

        MedicalConditionDb captured = captor.getValue();
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
        when(repository.save(any(MedicalConditionDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> {
            service.create("Name");
        });
        verify(repository).save(any(MedicalConditionDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        String name = "Updated Name";

        MedicalConditionDb existingDb = DomainBuilderDatabase.getConditionDb("Old Name", extid);
        MedicalConditionDb updatedDb = DomainBuilderDatabase.getConditionDb(name, extid);
        Condition expectedDomain = DomainBuilderDatabase.getCondition(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(MedicalConditionDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        Condition result = service.update(extid, name);

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<MedicalConditionDb> captor = ArgumentCaptor.forClass(MedicalConditionDb.class);
        verify(repository).save(captor.capture());

        MedicalConditionDb captured = captor.getValue();
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
            service.update(extid, "Name");
        });
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        // Arrange
        String extid = "existing-extid";
        MedicalConditionDb existingDb = DomainBuilderDatabase.getConditionDb("Name", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(MedicalConditionDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> {
            service.update(extid, "New Name");
        });
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        MedicalConditionDb existingDb = DomainBuilderDatabase.getConditionDb("Name", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(MedicalConditionDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<MedicalConditionDb> captor = ArgumentCaptor.forClass(MedicalConditionDb.class);
        verify(repository).save(captor.capture());

        MedicalConditionDb captured = captor.getValue();
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
    void findByExtid_shouldReturnCondition_whenExists() {
        // Arrange
        String extid = "existing-extid";
        MedicalConditionDb db = DomainBuilderDatabase.getConditionDb("Name", extid);
        Condition expectedDomain = DomainBuilderDatabase.getCondition(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        Condition result = service.findByExtid(extid);

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
    void findAll_shouldReturnAllConditions() {
        // Arrange
        MedicalConditionDb db1 = DomainBuilderDatabase.getConditionDb();
        MedicalConditionDb db2 = DomainBuilderDatabase.getConditionDb();
        List<MedicalConditionDb> dbList = Arrays.asList(db1, db2);

        Condition domain1 = DomainBuilderDatabase.getCondition(db1);
        Condition domain2 = DomainBuilderDatabase.getCondition(db2);
        List<Condition> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Condition> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredConditions() {
        // Arrange
        MedicalConditionDb db1 = DomainBuilderDatabase.getConditionDb();
        db1.setActive(ActiveEnum.ACTIVE);
        MedicalConditionDb db2 = DomainBuilderDatabase.getConditionDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<MedicalConditionDb> dbList = Arrays.asList(db1, db2);

        Condition domain1 = DomainBuilderDatabase.getCondition(db1);
        Condition domain2 = DomainBuilderDatabase.getCondition(db2);
        List<Condition> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(ActiveEnum.ACTIVE)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Condition> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByActive(ActiveEnum.ACTIVE);
        verify(mapper).toModelList(dbList);
    }
}
