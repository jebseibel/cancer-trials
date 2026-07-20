package com.seibel.jobhunting.database.db.service;

import com.seibel.jobhunting.common.domain.FriendJobPosting;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.common.exceptions.ServiceException;
import com.seibel.jobhunting.database.db.entity.FriendJobPostingDb;
import com.seibel.jobhunting.database.db.mapper.FriendJobPostingMapper;
import com.seibel.jobhunting.database.db.repository.FriendJobPostingRepository;
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
class FriendJobPostingDbServiceTest {

    @Mock
    private FriendJobPostingRepository repository;

    @Mock
    private FriendJobPostingMapper mapper;

    @InjectMocks
    private FriendJobPostingDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        Long friendId = 1L;
        Long jobPostingId = 2L;
        FriendJobPostingDb savedDb = DomainBuilderDatabase.getFriendJobPostingDb(friendId, jobPostingId, null);
        FriendJobPosting expectedDomain = DomainBuilderDatabase.getFriendJobPosting(savedDb);

        when(repository.save(any(FriendJobPostingDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        FriendJobPosting result = service.create(friendId, jobPostingId);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<FriendJobPostingDb> captor = ArgumentCaptor.forClass(FriendJobPostingDb.class);
        verify(repository).save(captor.capture());

        FriendJobPostingDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(friendId, captured.getFriendId());
        assertEquals(jobPostingId, captured.getJobPostingId());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        // Arrange
        when(repository.save(any(FriendJobPostingDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.create(1L, 2L));
        verify(repository).save(any(FriendJobPostingDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        Long friendId = 3L;
        Long jobPostingId = 4L;

        FriendJobPostingDb existingDb = DomainBuilderDatabase.getFriendJobPostingDb(1L, 2L, extid);
        FriendJobPostingDb updatedDb = DomainBuilderDatabase.getFriendJobPostingDb(friendId, jobPostingId, extid);
        FriendJobPosting expectedDomain = DomainBuilderDatabase.getFriendJobPosting(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(FriendJobPostingDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        FriendJobPosting result = service.update(extid, friendId, jobPostingId);

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<FriendJobPostingDb> captor = ArgumentCaptor.forClass(FriendJobPostingDb.class);
        verify(repository).save(captor.capture());

        FriendJobPostingDb captured = captor.getValue();
        assertEquals(friendId, captured.getFriendId());
        assertEquals(jobPostingId, captured.getJobPostingId());
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
        FriendJobPostingDb existingDb = DomainBuilderDatabase.getFriendJobPostingDb(1L, 2L, extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(FriendJobPostingDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.update(extid, 3L, 4L));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        FriendJobPostingDb existingDb = DomainBuilderDatabase.getFriendJobPostingDb(1L, 2L, extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(FriendJobPostingDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<FriendJobPostingDb> captor = ArgumentCaptor.forClass(FriendJobPostingDb.class);
        verify(repository).save(captor.capture());

        FriendJobPostingDb captured = captor.getValue();
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
    void findByExtid_shouldReturnFriendJobPosting_whenExists() {
        // Arrange
        String extid = "existing-extid";
        FriendJobPostingDb db = DomainBuilderDatabase.getFriendJobPostingDb(1L, 2L, extid);
        FriendJobPosting expectedDomain = DomainBuilderDatabase.getFriendJobPosting(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        FriendJobPosting result = service.findByExtid(extid);

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
    void findAll_shouldReturnAllFriendJobPostings() {
        // Arrange
        FriendJobPostingDb db1 = DomainBuilderDatabase.getFriendJobPostingDb();
        FriendJobPostingDb db2 = DomainBuilderDatabase.getFriendJobPostingDb();
        List<FriendJobPostingDb> dbList = Arrays.asList(db1, db2);

        FriendJobPosting domain1 = DomainBuilderDatabase.getFriendJobPosting(db1);
        FriendJobPosting domain2 = DomainBuilderDatabase.getFriendJobPosting(db2);
        List<FriendJobPosting> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<FriendJobPosting> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredFriendJobPostings() {
        // Arrange
        FriendJobPostingDb db1 = DomainBuilderDatabase.getFriendJobPostingDb();
        db1.setActive(ActiveEnum.ACTIVE);
        FriendJobPostingDb db2 = DomainBuilderDatabase.getFriendJobPostingDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<FriendJobPostingDb> dbList = Arrays.asList(db1, db2);

        FriendJobPosting domain1 = DomainBuilderDatabase.getFriendJobPosting(db1);
        FriendJobPosting domain2 = DomainBuilderDatabase.getFriendJobPosting(db2);
        List<FriendJobPosting> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(ActiveEnum.ACTIVE)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<FriendJobPosting> result = service.findByActive(ActiveEnum.ACTIVE);

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
        FriendJobPostingDb db1 = DomainBuilderDatabase.getFriendJobPostingDb();
        List<FriendJobPostingDb> dbList = List.of(db1);

        FriendJobPosting domain1 = DomainBuilderDatabase.getFriendJobPosting(db1);
        List<FriendJobPosting> domainList = List.of(domain1);

        when(repository.findByFriendId(friendId)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<FriendJobPosting> result = service.findByFriendId(friendId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository).findByFriendId(friendId);
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByJobPostingId_shouldReturnMatchingLinks() {
        // Arrange
        Long jobPostingId = 42L;
        FriendJobPostingDb db1 = DomainBuilderDatabase.getFriendJobPostingDb();
        List<FriendJobPostingDb> dbList = List.of(db1);

        FriendJobPosting domain1 = DomainBuilderDatabase.getFriendJobPosting(db1);
        List<FriendJobPosting> domainList = List.of(domain1);

        when(repository.findByJobPostingId(jobPostingId)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<FriendJobPosting> result = service.findByJobPostingId(jobPostingId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository).findByJobPostingId(jobPostingId);
        verify(mapper).toModelList(dbList);
    }
}
