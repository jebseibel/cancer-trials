package com.seibel.basic.database.db.service;

import com.seibel.basic.common.domain.User;
import com.seibel.basic.common.enums.ActiveEnum;
import com.seibel.basic.database.db.entity.UserDb;
import com.seibel.basic.common.exceptions.ServiceException;
import com.seibel.basic.database.db.mapper.UserMapper;
import com.seibel.basic.database.db.repository.UserRepository;
import com.seibel.basic.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDbServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private UserMapper mapper;

    @InjectMocks
    private UserDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        String username = "testuser";
        String password = "password123";
        String email = "test@example.com";
        String role = "ADMIN";
        UserDb savedDb = DomainBuilderDatabase.getUserDb(username, password, email, role, null);
        User expectedDomain = DomainBuilderDatabase.getUser(savedDb);

        when(repository.save(any(UserDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        User result = service.create(username, password, email, role);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<UserDb> captor = ArgumentCaptor.forClass(UserDb.class);
        verify(repository).save(captor.capture());

        UserDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(username, captured.getUsername());
        assertEquals(password, captured.getPassword());
        assertEquals(email, captured.getEmail());
        assertEquals(role, captured.getRole());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        // Arrange
        when(repository.save(any(UserDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> {
            service.create("username", "password", "email@test.com", "USER");
        });
        verify(repository).save(any(UserDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        String username = "updated_user";
        String password = "newpassword";
        String email = "updated@example.com";
        String role = "ADMIN";

        UserDb existingDb = DomainBuilderDatabase.getUserDb("old_user", "oldpass", "old@example.com", "USER", extid);
        UserDb updatedDb = DomainBuilderDatabase.getUserDb(username, password, email, role, extid);
        User expectedDomain = DomainBuilderDatabase.getUser(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(UserDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        User result = service.update(extid, username, password, email, role);

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<UserDb> captor = ArgumentCaptor.forClass(UserDb.class);
        verify(repository).save(captor.capture());

        UserDb captured = captor.getValue();
        assertEquals(username, captured.getUsername());
        assertEquals(password, captured.getPassword());
        assertEquals(email, captured.getEmail());
        assertEquals(role, captured.getRole());
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
            service.update(extid, "user", "pass", "email@test.com", "USER");
        });
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        // Arrange
        String extid = "existing-extid";
        UserDb existingDb = DomainBuilderDatabase.getUserDb("user", "pass", "email@test.com", "USER", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(UserDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> {
            service.update(extid, "newuser", "newpass", "new@test.com", "ADMIN");
        });
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        UserDb existingDb = DomainBuilderDatabase.getUserDb("user", "pass", "email@test.com", "USER", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(UserDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<UserDb> captor = ArgumentCaptor.forClass(UserDb.class);
        verify(repository).save(captor.capture());

        UserDb captured = captor.getValue();
        assertNotNull(captured.getDeletedAt());
        assertEquals(ActiveEnum.INACTIVE, captured.getActive());
    }

    @Test
    void delete_shouldThrowException_whenNotFound() {
        // Arrange
        String extid = "nonexistent-extid";
        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ServiceException.class, () -> {
            service.delete(extid);
        });
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
    }

    @Test
    void findByExtid_shouldReturnUser_whenExists() {
        // Arrange
        String extid = "existing-extid";
        UserDb db = DomainBuilderDatabase.getUserDb("user", "pass", "email@test.com", "USER", extid);
        User expectedDomain = DomainBuilderDatabase.getUser(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        User result = service.findByExtid(extid);

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
    void findAll_shouldReturnAllUsers() {
        // Arrange
        UserDb db1 = DomainBuilderDatabase.getUserDb();
        UserDb db2 = DomainBuilderDatabase.getUserDb();
        List<UserDb> dbList = Arrays.asList(db1, db2);

        User domain1 = DomainBuilderDatabase.getUser(db1);
        User domain2 = DomainBuilderDatabase.getUser(db2);
        List<User> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<User> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredUsers() {
        // Arrange
        UserDb db1 = DomainBuilderDatabase.getUserDb();
        db1.setActive(ActiveEnum.ACTIVE);
        UserDb db2 = DomainBuilderDatabase.getUserDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<UserDb> dbList = Arrays.asList(db1, db2);

        User domain1 = DomainBuilderDatabase.getUser(db1);
        User domain2 = DomainBuilderDatabase.getUser(db2);
        List<User> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(any(ActiveEnum.class))).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<User> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByActive(any(ActiveEnum.class));
        verify(mapper).toModelList(dbList);
    }
}
