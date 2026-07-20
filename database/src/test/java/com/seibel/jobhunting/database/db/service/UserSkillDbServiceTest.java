package com.seibel.jobhunting.database.db.service;

import com.seibel.jobhunting.common.domain.UserSkill;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.common.exceptions.ServiceException;
import com.seibel.jobhunting.database.db.entity.UserSkillDb;
import com.seibel.jobhunting.database.db.mapper.UserSkillMapper;
import com.seibel.jobhunting.database.db.repository.UserSkillRepository;
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
class UserSkillDbServiceTest {

    @Mock
    private UserSkillRepository repository;

    @Mock
    private UserSkillMapper mapper;

    @InjectMocks
    private UserSkillDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        Long userId = 1L;
        Long skillId = 2L;
        UserSkillDb savedDb = DomainBuilderDatabase.getUserSkillDb(userId, skillId, null);
        UserSkill expectedDomain = DomainBuilderDatabase.getUserSkill(savedDb);

        when(repository.save(any(UserSkillDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        UserSkill result = service.create(userId, skillId);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<UserSkillDb> captor = ArgumentCaptor.forClass(UserSkillDb.class);
        verify(repository).save(captor.capture());

        UserSkillDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(userId, captured.getUserId());
        assertEquals(skillId, captured.getSkillId());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        // Arrange
        when(repository.save(any(UserSkillDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.create(1L, 2L));
        verify(repository).save(any(UserSkillDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        Long userId = 3L;
        Long skillId = 4L;

        UserSkillDb existingDb = DomainBuilderDatabase.getUserSkillDb(1L, 2L, extid);
        UserSkillDb updatedDb = DomainBuilderDatabase.getUserSkillDb(userId, skillId, extid);
        UserSkill expectedDomain = DomainBuilderDatabase.getUserSkill(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(UserSkillDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        UserSkill result = service.update(extid, userId, skillId);

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<UserSkillDb> captor = ArgumentCaptor.forClass(UserSkillDb.class);
        verify(repository).save(captor.capture());

        UserSkillDb captured = captor.getValue();
        assertEquals(userId, captured.getUserId());
        assertEquals(skillId, captured.getSkillId());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(updatedDb);
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        // Arrange
        String extid = "nonexistent-extid";
        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.update(extid, 1L, 2L));
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        // Arrange
        String extid = "existing-extid";
        UserSkillDb existingDb = DomainBuilderDatabase.getUserSkillDb(1L, 2L, extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(UserSkillDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.update(extid, 3L, 4L));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        UserSkillDb existingDb = DomainBuilderDatabase.getUserSkillDb(1L, 2L, extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(UserSkillDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<UserSkillDb> captor = ArgumentCaptor.forClass(UserSkillDb.class);
        verify(repository).save(captor.capture());

        UserSkillDb captured = captor.getValue();
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
    void findByExtid_shouldReturnUserSkill_whenExists() {
        // Arrange
        String extid = "existing-extid";
        UserSkillDb db = DomainBuilderDatabase.getUserSkillDb(1L, 2L, extid);
        UserSkill expectedDomain = DomainBuilderDatabase.getUserSkill(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        UserSkill result = service.findByExtid(extid);

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
    void findAll_shouldReturnAllUserSkills() {
        // Arrange
        UserSkillDb db1 = DomainBuilderDatabase.getUserSkillDb();
        UserSkillDb db2 = DomainBuilderDatabase.getUserSkillDb();
        List<UserSkillDb> dbList = Arrays.asList(db1, db2);

        UserSkill domain1 = DomainBuilderDatabase.getUserSkill(db1);
        UserSkill domain2 = DomainBuilderDatabase.getUserSkill(db2);
        List<UserSkill> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<UserSkill> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredUserSkills() {
        // Arrange
        UserSkillDb db1 = DomainBuilderDatabase.getUserSkillDb();
        db1.setActive(ActiveEnum.ACTIVE);
        UserSkillDb db2 = DomainBuilderDatabase.getUserSkillDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<UserSkillDb> dbList = Arrays.asList(db1, db2);

        UserSkill domain1 = DomainBuilderDatabase.getUserSkill(db1);
        UserSkill domain2 = DomainBuilderDatabase.getUserSkill(db2);
        List<UserSkill> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(ActiveEnum.ACTIVE)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<UserSkill> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByActive(ActiveEnum.ACTIVE);
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByUserId_shouldReturnMatchingLinks() {
        // Arrange
        Long userId = 42L;
        UserSkillDb db1 = DomainBuilderDatabase.getUserSkillDb();
        List<UserSkillDb> dbList = List.of(db1);

        UserSkill domain1 = DomainBuilderDatabase.getUserSkill(db1);
        List<UserSkill> domainList = List.of(domain1);

        when(repository.findByUserId(userId)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<UserSkill> result = service.findByUserId(userId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository).findByUserId(userId);
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findBySkillId_shouldReturnMatchingLinks() {
        // Arrange
        Long skillId = 42L;
        UserSkillDb db1 = DomainBuilderDatabase.getUserSkillDb();
        List<UserSkillDb> dbList = List.of(db1);

        UserSkill domain1 = DomainBuilderDatabase.getUserSkill(db1);
        List<UserSkill> domainList = List.of(domain1);

        when(repository.findBySkillId(skillId)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<UserSkill> result = service.findBySkillId(skillId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository).findBySkillId(skillId);
        verify(mapper).toModelList(dbList);
    }
}
