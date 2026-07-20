package com.seibel.jobhunting.database.db.service;

import com.seibel.jobhunting.common.domain.FriendSkill;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.common.exceptions.ServiceException;
import com.seibel.jobhunting.database.db.entity.FriendSkillDb;
import com.seibel.jobhunting.database.db.mapper.FriendSkillMapper;
import com.seibel.jobhunting.database.db.repository.FriendSkillRepository;
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
class FriendSkillDbServiceTest {

    @Mock
    private FriendSkillRepository repository;

    @Mock
    private FriendSkillMapper mapper;

    @InjectMocks
    private FriendSkillDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        Long friendId = 1L;
        Long skillId = 2L;
        FriendSkillDb savedDb = DomainBuilderDatabase.getFriendSkillDb(friendId, skillId, null);
        FriendSkill expectedDomain = DomainBuilderDatabase.getFriendSkill(savedDb);

        when(repository.save(any(FriendSkillDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        FriendSkill result = service.create(friendId, skillId);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<FriendSkillDb> captor = ArgumentCaptor.forClass(FriendSkillDb.class);
        verify(repository).save(captor.capture());

        FriendSkillDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(friendId, captured.getFriendId());
        assertEquals(skillId, captured.getSkillId());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        // Arrange
        when(repository.save(any(FriendSkillDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.create(1L, 2L));
        verify(repository).save(any(FriendSkillDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        Long friendId = 3L;
        Long skillId = 4L;

        FriendSkillDb existingDb = DomainBuilderDatabase.getFriendSkillDb(1L, 2L, extid);
        FriendSkillDb updatedDb = DomainBuilderDatabase.getFriendSkillDb(friendId, skillId, extid);
        FriendSkill expectedDomain = DomainBuilderDatabase.getFriendSkill(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(FriendSkillDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        FriendSkill result = service.update(extid, friendId, skillId);

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<FriendSkillDb> captor = ArgumentCaptor.forClass(FriendSkillDb.class);
        verify(repository).save(captor.capture());

        FriendSkillDb captured = captor.getValue();
        assertEquals(friendId, captured.getFriendId());
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
        FriendSkillDb existingDb = DomainBuilderDatabase.getFriendSkillDb(1L, 2L, extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(FriendSkillDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.update(extid, 3L, 4L));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        FriendSkillDb existingDb = DomainBuilderDatabase.getFriendSkillDb(1L, 2L, extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(FriendSkillDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<FriendSkillDb> captor = ArgumentCaptor.forClass(FriendSkillDb.class);
        verify(repository).save(captor.capture());

        FriendSkillDb captured = captor.getValue();
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
    void findByExtid_shouldReturnFriendSkill_whenExists() {
        // Arrange
        String extid = "existing-extid";
        FriendSkillDb db = DomainBuilderDatabase.getFriendSkillDb(1L, 2L, extid);
        FriendSkill expectedDomain = DomainBuilderDatabase.getFriendSkill(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        FriendSkill result = service.findByExtid(extid);

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
    void findAll_shouldReturnAllFriendSkills() {
        // Arrange
        FriendSkillDb db1 = DomainBuilderDatabase.getFriendSkillDb();
        FriendSkillDb db2 = DomainBuilderDatabase.getFriendSkillDb();
        List<FriendSkillDb> dbList = Arrays.asList(db1, db2);

        FriendSkill domain1 = DomainBuilderDatabase.getFriendSkill(db1);
        FriendSkill domain2 = DomainBuilderDatabase.getFriendSkill(db2);
        List<FriendSkill> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<FriendSkill> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredFriendSkills() {
        // Arrange
        FriendSkillDb db1 = DomainBuilderDatabase.getFriendSkillDb();
        db1.setActive(ActiveEnum.ACTIVE);
        FriendSkillDb db2 = DomainBuilderDatabase.getFriendSkillDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<FriendSkillDb> dbList = Arrays.asList(db1, db2);

        FriendSkill domain1 = DomainBuilderDatabase.getFriendSkill(db1);
        FriendSkill domain2 = DomainBuilderDatabase.getFriendSkill(db2);
        List<FriendSkill> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(ActiveEnum.ACTIVE)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<FriendSkill> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByActive(ActiveEnum.ACTIVE);
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByFriendId_shouldReturnMatchingLinks() {
        // Arrange
        Long friendId = 42L;
        FriendSkillDb db1 = DomainBuilderDatabase.getFriendSkillDb();
        List<FriendSkillDb> dbList = List.of(db1);

        FriendSkill domain1 = DomainBuilderDatabase.getFriendSkill(db1);
        List<FriendSkill> domainList = List.of(domain1);

        when(repository.findByFriendId(friendId)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<FriendSkill> result = service.findByFriendId(friendId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository).findByFriendId(friendId);
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findBySkillId_shouldReturnMatchingLinks() {
        // Arrange
        Long skillId = 42L;
        FriendSkillDb db1 = DomainBuilderDatabase.getFriendSkillDb();
        List<FriendSkillDb> dbList = List.of(db1);

        FriendSkill domain1 = DomainBuilderDatabase.getFriendSkill(db1);
        List<FriendSkill> domainList = List.of(domain1);

        when(repository.findBySkillId(skillId)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<FriendSkill> result = service.findBySkillId(skillId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository).findBySkillId(skillId);
        verify(mapper).toModelList(dbList);
    }
}
