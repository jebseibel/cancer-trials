package com.seibel.jobhunting.database.db.service;

import com.seibel.jobhunting.common.domain.FriendCompany;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.common.exceptions.ServiceException;
import com.seibel.jobhunting.database.db.entity.FriendCompanyDb;
import com.seibel.jobhunting.database.db.mapper.FriendCompanyMapper;
import com.seibel.jobhunting.database.db.repository.FriendCompanyRepository;
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
class FriendCompanyDbServiceTest {

    @Mock
    private FriendCompanyRepository repository;

    @Mock
    private FriendCompanyMapper mapper;

    @InjectMocks
    private FriendCompanyDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        Long friendId = 1L;
        Long companyId = 2L;
        FriendCompanyDb savedDb = DomainBuilderDatabase.getFriendCompanyDb(friendId, companyId, null);
        FriendCompany expectedDomain = DomainBuilderDatabase.getFriendCompany(savedDb);

        when(repository.save(any(FriendCompanyDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        FriendCompany result = service.create(friendId, companyId);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<FriendCompanyDb> captor = ArgumentCaptor.forClass(FriendCompanyDb.class);
        verify(repository).save(captor.capture());

        FriendCompanyDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(friendId, captured.getFriendId());
        assertEquals(companyId, captured.getCompanyId());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        // Arrange
        when(repository.save(any(FriendCompanyDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.create(1L, 2L));
        verify(repository).save(any(FriendCompanyDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        Long friendId = 3L;
        Long companyId = 4L;

        FriendCompanyDb existingDb = DomainBuilderDatabase.getFriendCompanyDb(1L, 2L, extid);
        FriendCompanyDb updatedDb = DomainBuilderDatabase.getFriendCompanyDb(friendId, companyId, extid);
        FriendCompany expectedDomain = DomainBuilderDatabase.getFriendCompany(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(FriendCompanyDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        FriendCompany result = service.update(extid, friendId, companyId);

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<FriendCompanyDb> captor = ArgumentCaptor.forClass(FriendCompanyDb.class);
        verify(repository).save(captor.capture());

        FriendCompanyDb captured = captor.getValue();
        assertEquals(friendId, captured.getFriendId());
        assertEquals(companyId, captured.getCompanyId());
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
        FriendCompanyDb existingDb = DomainBuilderDatabase.getFriendCompanyDb(1L, 2L, extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(FriendCompanyDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.update(extid, 3L, 4L));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        FriendCompanyDb existingDb = DomainBuilderDatabase.getFriendCompanyDb(1L, 2L, extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(FriendCompanyDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<FriendCompanyDb> captor = ArgumentCaptor.forClass(FriendCompanyDb.class);
        verify(repository).save(captor.capture());

        FriendCompanyDb captured = captor.getValue();
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
    void findByExtid_shouldReturnFriendCompany_whenExists() {
        // Arrange
        String extid = "existing-extid";
        FriendCompanyDb db = DomainBuilderDatabase.getFriendCompanyDb(1L, 2L, extid);
        FriendCompany expectedDomain = DomainBuilderDatabase.getFriendCompany(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        FriendCompany result = service.findByExtid(extid);

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
    void findAll_shouldReturnAllFriendCompanies() {
        // Arrange
        FriendCompanyDb db1 = DomainBuilderDatabase.getFriendCompanyDb();
        FriendCompanyDb db2 = DomainBuilderDatabase.getFriendCompanyDb();
        List<FriendCompanyDb> dbList = Arrays.asList(db1, db2);

        FriendCompany domain1 = DomainBuilderDatabase.getFriendCompany(db1);
        FriendCompany domain2 = DomainBuilderDatabase.getFriendCompany(db2);
        List<FriendCompany> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<FriendCompany> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredFriendCompanies() {
        // Arrange
        FriendCompanyDb db1 = DomainBuilderDatabase.getFriendCompanyDb();
        db1.setActive(ActiveEnum.ACTIVE);
        FriendCompanyDb db2 = DomainBuilderDatabase.getFriendCompanyDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<FriendCompanyDb> dbList = Arrays.asList(db1, db2);

        FriendCompany domain1 = DomainBuilderDatabase.getFriendCompany(db1);
        FriendCompany domain2 = DomainBuilderDatabase.getFriendCompany(db2);
        List<FriendCompany> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(ActiveEnum.ACTIVE)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<FriendCompany> result = service.findByActive(ActiveEnum.ACTIVE);

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
        FriendCompanyDb db1 = DomainBuilderDatabase.getFriendCompanyDb();
        List<FriendCompanyDb> dbList = List.of(db1);

        FriendCompany domain1 = DomainBuilderDatabase.getFriendCompany(db1);
        List<FriendCompany> domainList = List.of(domain1);

        when(repository.findByFriendId(friendId)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<FriendCompany> result = service.findByFriendId(friendId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository).findByFriendId(friendId);
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByCompanyId_shouldReturnMatchingLinks() {
        // Arrange
        Long companyId = 42L;
        FriendCompanyDb db1 = DomainBuilderDatabase.getFriendCompanyDb();
        List<FriendCompanyDb> dbList = List.of(db1);

        FriendCompany domain1 = DomainBuilderDatabase.getFriendCompany(db1);
        List<FriendCompany> domainList = List.of(domain1);

        when(repository.findByCompanyId(companyId)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<FriendCompany> result = service.findByCompanyId(companyId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository).findByCompanyId(companyId);
        verify(mapper).toModelList(dbList);
    }
}
