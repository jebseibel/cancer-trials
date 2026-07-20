package com.seibel.jobhunting.database.db.service;

import com.seibel.jobhunting.common.domain.Skill;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.common.exceptions.ServiceException;
import com.seibel.jobhunting.database.db.entity.SkillDb;
import com.seibel.jobhunting.database.db.mapper.SkillMapper;
import com.seibel.jobhunting.database.db.repository.SkillRepository;
import com.seibel.jobhunting.testutils.DomainBuilderDatabase;
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
class SkillDbServiceTest {

    @Mock
    private SkillRepository repository;

    @Mock
    private SkillMapper mapper;

    @InjectMocks
    private SkillDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        String name = "Java";
        SkillDb savedDb = DomainBuilderDatabase.getSkillDb(name, null);
        Skill expectedDomain = DomainBuilderDatabase.getSkill(savedDb);

        when(repository.save(any(SkillDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        Skill result = service.create(name);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<SkillDb> captor = ArgumentCaptor.forClass(SkillDb.class);
        verify(repository).save(captor.capture());

        SkillDb captured = captor.getValue();
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
        when(repository.save(any(SkillDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.create("Java"));
        verify(repository).save(any(SkillDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        String name = "Kubernetes";

        SkillDb existingDb = DomainBuilderDatabase.getSkillDb("Docker", extid);
        SkillDb updatedDb = DomainBuilderDatabase.getSkillDb(name, extid);
        Skill expectedDomain = DomainBuilderDatabase.getSkill(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(SkillDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        Skill result = service.update(extid, name);

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<SkillDb> captor = ArgumentCaptor.forClass(SkillDb.class);
        verify(repository).save(captor.capture());

        SkillDb captured = captor.getValue();
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
        assertThrows(ServiceException.class, () -> service.update(extid, "Python"));
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        // Arrange
        String extid = "existing-extid";
        SkillDb existingDb = DomainBuilderDatabase.getSkillDb("Java", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(SkillDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.update(extid, "New Skill"));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        SkillDb existingDb = DomainBuilderDatabase.getSkillDb("Java", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(SkillDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<SkillDb> captor = ArgumentCaptor.forClass(SkillDb.class);
        verify(repository).save(captor.capture());

        SkillDb captured = captor.getValue();
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
    void findByExtid_shouldReturnSkill_whenExists() {
        // Arrange
        String extid = "existing-extid";
        SkillDb db = DomainBuilderDatabase.getSkillDb("Java", extid);
        Skill expectedDomain = DomainBuilderDatabase.getSkill(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        Skill result = service.findByExtid(extid);

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
    void findAll_shouldReturnAllSkills() {
        // Arrange
        SkillDb db1 = DomainBuilderDatabase.getSkillDb();
        SkillDb db2 = DomainBuilderDatabase.getSkillDb();
        List<SkillDb> dbList = Arrays.asList(db1, db2);

        Skill domain1 = DomainBuilderDatabase.getSkill(db1);
        Skill domain2 = DomainBuilderDatabase.getSkill(db2);
        List<Skill> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Skill> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredSkills() {
        // Arrange
        SkillDb db1 = DomainBuilderDatabase.getSkillDb();
        db1.setActive(ActiveEnum.ACTIVE);
        SkillDb db2 = DomainBuilderDatabase.getSkillDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<SkillDb> dbList = Arrays.asList(db1, db2);

        Skill domain1 = DomainBuilderDatabase.getSkill(db1);
        Skill domain2 = DomainBuilderDatabase.getSkill(db2);
        List<Skill> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(ActiveEnum.ACTIVE)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Skill> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByActive(ActiveEnum.ACTIVE);
        verify(mapper).toModelList(dbList);
    }
}
