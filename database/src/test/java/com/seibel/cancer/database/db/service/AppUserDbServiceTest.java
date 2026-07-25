package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.AppUser;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.AppUserDb;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.mapper.AppUserMapper;
import com.seibel.cancer.database.db.repository.AppUserRepository;
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
class AppUserDbServiceTest {

    @Mock
    private AppUserRepository repository;

    @Mock
    private AppUserMapper mapper;

    @InjectMocks
    private AppUserDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        String username = "testuser";
        String passwordHash = "hashed-password-123";
        String displayName = "Test User";
        AppUserDb savedDb = DomainBuilderDatabase.getAppUserDb(username, passwordHash, displayName, null);
        AppUser expectedDomain = DomainBuilderDatabase.getAppUser(savedDb);

        when(repository.save(any(AppUserDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        AppUser result = service.create(username, passwordHash, displayName);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<AppUserDb> captor = ArgumentCaptor.forClass(AppUserDb.class);
        verify(repository).save(captor.capture());

        AppUserDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(username, captured.getUsername());
        assertEquals(passwordHash, captured.getPasswordHash());
        assertEquals(displayName, captured.getDisplayName());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        // Arrange
        when(repository.save(any(AppUserDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> {
            service.create("username", "passwordHash", "Display Name");
        });
        verify(repository).save(any(AppUserDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        String username = "updated_user";
        String passwordHash = "new-hashed-password";
        String displayName = "Updated Name";

        AppUserDb existingDb = DomainBuilderDatabase.getAppUserDb("old_user", "old-hash", "Old Name", extid);
        AppUserDb updatedDb = DomainBuilderDatabase.getAppUserDb(username, passwordHash, displayName, extid);
        AppUser expectedDomain = DomainBuilderDatabase.getAppUser(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(AppUserDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        AppUser result = service.update(extid, username, passwordHash, displayName);

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<AppUserDb> captor = ArgumentCaptor.forClass(AppUserDb.class);
        verify(repository).save(captor.capture());

        AppUserDb captured = captor.getValue();
        assertEquals(username, captured.getUsername());
        assertEquals(passwordHash, captured.getPasswordHash());
        assertEquals(displayName, captured.getDisplayName());
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
            service.update(extid, "user", "hash", "Name");
        });
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        // Arrange
        String extid = "existing-extid";
        AppUserDb existingDb = DomainBuilderDatabase.getAppUserDb("user", "hash", "Name", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(AppUserDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> {
            service.update(extid, "newuser", "newhash", "New Name");
        });
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        AppUserDb existingDb = DomainBuilderDatabase.getAppUserDb("user", "hash", "Name", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(AppUserDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<AppUserDb> captor = ArgumentCaptor.forClass(AppUserDb.class);
        verify(repository).save(captor.capture());

        AppUserDb captured = captor.getValue();
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
    void findByExtid_shouldReturnAppUser_whenExists() {
        // Arrange
        String extid = "existing-extid";
        AppUserDb db = DomainBuilderDatabase.getAppUserDb("user", "hash", "Name", extid);
        AppUser expectedDomain = DomainBuilderDatabase.getAppUser(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        AppUser result = service.findByExtid(extid);

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
        AppUserDb db1 = DomainBuilderDatabase.getAppUserDb();
        AppUserDb db2 = DomainBuilderDatabase.getAppUserDb();
        List<AppUserDb> dbList = Arrays.asList(db1, db2);

        AppUser domain1 = DomainBuilderDatabase.getAppUser(db1);
        AppUser domain2 = DomainBuilderDatabase.getAppUser(db2);
        List<AppUser> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<AppUser> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredEntities() {
        // Arrange
        AppUserDb db1 = DomainBuilderDatabase.getAppUserDb();
        db1.setActive(ActiveEnum.ACTIVE);
        AppUserDb db2 = DomainBuilderDatabase.getAppUserDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<AppUserDb> dbList = Arrays.asList(db1, db2);

        AppUser domain1 = DomainBuilderDatabase.getAppUser(db1);
        AppUser domain2 = DomainBuilderDatabase.getAppUser(db2);
        List<AppUser> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(any(ActiveEnum.class))).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<AppUser> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByActive(any(ActiveEnum.class));
        verify(mapper).toModelList(dbList);
    }
}
