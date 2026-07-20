package com.seibel.jobhunting.database.db.service;

import com.seibel.jobhunting.common.domain.Friend;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.common.exceptions.ServiceException;
import com.seibel.jobhunting.database.db.entity.FriendDb;
import com.seibel.jobhunting.database.db.mapper.FriendMapper;
import com.seibel.jobhunting.database.db.repository.FriendRepository;
import com.seibel.jobhunting.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendDbServiceTest {

    @Mock
    private FriendRepository repository;

    @Mock
    private FriendMapper mapper;

    @InjectMocks
    private FriendDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        String name = "Jane Doe";
        FriendDb savedDb = DomainBuilderDatabase.getFriendDb(name, null);
        Friend expectedDomain = DomainBuilderDatabase.getFriend(savedDb);

        when(repository.save(any(FriendDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        Friend result = service.create(name, "Former coworker", "jane@example.com", "555-1234",
                "https://linkedin.com/in/jane", LocalDate.now(), "Notes");

        // Assert
        assertNotNull(result);
        ArgumentCaptor<FriendDb> captor = ArgumentCaptor.forClass(FriendDb.class);
        verify(repository).save(captor.capture());

        FriendDb captured = captor.getValue();
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
        when(repository.save(any(FriendDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () ->
                service.create("Jane Doe", null, null, null, null, null, null));
        verify(repository).save(any(FriendDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        String name = "Updated Name";

        FriendDb existingDb = DomainBuilderDatabase.getFriendDb("Old Name", extid);
        FriendDb updatedDb = DomainBuilderDatabase.getFriendDb(name, extid);
        Friend expectedDomain = DomainBuilderDatabase.getFriend(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(FriendDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        Friend result = service.update(extid, name, "College friend", "new@example.com", "555-5678",
                "https://linkedin.com/in/new", LocalDate.now(), "New notes");

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<FriendDb> captor = ArgumentCaptor.forClass(FriendDb.class);
        verify(repository).save(captor.capture());

        FriendDb captured = captor.getValue();
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
        assertThrows(ServiceException.class, () ->
                service.update(extid, "Name", null, null, null, null, null, null));
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        // Arrange
        String extid = "existing-extid";
        FriendDb existingDb = DomainBuilderDatabase.getFriendDb("Jane", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(FriendDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () ->
                service.update(extid, "New Name", null, null, null, null, null, null));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        FriendDb existingDb = DomainBuilderDatabase.getFriendDb("Jane", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(FriendDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<FriendDb> captor = ArgumentCaptor.forClass(FriendDb.class);
        verify(repository).save(captor.capture());

        FriendDb captured = captor.getValue();
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
    void findByExtid_shouldReturnFriend_whenExists() {
        // Arrange
        String extid = "existing-extid";
        FriendDb db = DomainBuilderDatabase.getFriendDb("Jane", extid);
        Friend expectedDomain = DomainBuilderDatabase.getFriend(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        Friend result = service.findByExtid(extid);

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
    void findAll_shouldReturnAllFriends() {
        // Arrange
        FriendDb db1 = DomainBuilderDatabase.getFriendDb();
        FriendDb db2 = DomainBuilderDatabase.getFriendDb();
        List<FriendDb> dbList = Arrays.asList(db1, db2);

        Friend domain1 = DomainBuilderDatabase.getFriend(db1);
        Friend domain2 = DomainBuilderDatabase.getFriend(db2);
        List<Friend> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Friend> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredFriends() {
        // Arrange
        FriendDb db1 = DomainBuilderDatabase.getFriendDb();
        db1.setActive(ActiveEnum.ACTIVE);
        FriendDb db2 = DomainBuilderDatabase.getFriendDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<FriendDb> dbList = Arrays.asList(db1, db2);

        Friend domain1 = DomainBuilderDatabase.getFriend(db1);
        Friend domain2 = DomainBuilderDatabase.getFriend(db2);
        List<Friend> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(ActiveEnum.ACTIVE)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Friend> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByActive(ActiveEnum.ACTIVE);
        verify(mapper).toModelList(dbList);
    }
}
