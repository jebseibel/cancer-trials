package com.seibel.jobhunting.database.db.service;

import com.seibel.jobhunting.common.domain.Application;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.common.enums.ApplicationStatus;
import com.seibel.jobhunting.common.exceptions.ServiceException;
import com.seibel.jobhunting.database.db.entity.ApplicationDb;
import com.seibel.jobhunting.database.db.mapper.ApplicationMapper;
import com.seibel.jobhunting.database.db.repository.ApplicationRepository;
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
class ApplicationDbServiceTest {

    @Mock
    private ApplicationRepository repository;

    @Mock
    private ApplicationMapper mapper;

    @InjectMocks
    private ApplicationDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        Long jobPostingId = 1L;
        ApplicationDb savedDb = DomainBuilderDatabase.getApplicationDb(jobPostingId, null);
        Application expectedDomain = DomainBuilderDatabase.getApplication(savedDb);

        when(repository.save(any(ApplicationDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        Application result = service.create(jobPostingId, LocalDate.now(), "v1", ApplicationStatus.APPLIED, "Notes");

        // Assert
        assertNotNull(result);
        ArgumentCaptor<ApplicationDb> captor = ArgumentCaptor.forClass(ApplicationDb.class);
        verify(repository).save(captor.capture());

        ApplicationDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(jobPostingId, captured.getJobPostingId());
        assertEquals(ApplicationStatus.APPLIED, captured.getApplicationStatus());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        // Arrange
        when(repository.save(any(ApplicationDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () ->
                service.create(1L, LocalDate.now(), null, ApplicationStatus.APPLIED, null));
        verify(repository).save(any(ApplicationDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        Long jobPostingId = 2L;

        ApplicationDb existingDb = DomainBuilderDatabase.getApplicationDb(1L, extid);
        ApplicationDb updatedDb = DomainBuilderDatabase.getApplicationDb(jobPostingId, extid);
        Application expectedDomain = DomainBuilderDatabase.getApplication(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(ApplicationDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        Application result = service.update(extid, jobPostingId, LocalDate.now(), "v2", ApplicationStatus.INTERVIEWING, "New notes");

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<ApplicationDb> captor = ArgumentCaptor.forClass(ApplicationDb.class);
        verify(repository).save(captor.capture());

        ApplicationDb captured = captor.getValue();
        assertEquals(jobPostingId, captured.getJobPostingId());
        assertEquals(ApplicationStatus.INTERVIEWING, captured.getApplicationStatus());
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
                service.update(extid, 1L, null, null, null, null));
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        // Arrange
        String extid = "existing-extid";
        ApplicationDb existingDb = DomainBuilderDatabase.getApplicationDb(1L, extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(ApplicationDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () ->
                service.update(extid, 2L, null, null, ApplicationStatus.OFFER, null));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        ApplicationDb existingDb = DomainBuilderDatabase.getApplicationDb(1L, extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(ApplicationDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<ApplicationDb> captor = ArgumentCaptor.forClass(ApplicationDb.class);
        verify(repository).save(captor.capture());

        ApplicationDb captured = captor.getValue();
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
    void findByExtid_shouldReturnApplication_whenExists() {
        // Arrange
        String extid = "existing-extid";
        ApplicationDb db = DomainBuilderDatabase.getApplicationDb(1L, extid);
        Application expectedDomain = DomainBuilderDatabase.getApplication(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        Application result = service.findByExtid(extid);

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
    void findAll_shouldReturnAllApplications() {
        // Arrange
        ApplicationDb db1 = DomainBuilderDatabase.getApplicationDb();
        ApplicationDb db2 = DomainBuilderDatabase.getApplicationDb();
        List<ApplicationDb> dbList = Arrays.asList(db1, db2);

        Application domain1 = DomainBuilderDatabase.getApplication(db1);
        Application domain2 = DomainBuilderDatabase.getApplication(db2);
        List<Application> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Application> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredApplications() {
        // Arrange
        ApplicationDb db1 = DomainBuilderDatabase.getApplicationDb();
        db1.setActive(ActiveEnum.ACTIVE);
        ApplicationDb db2 = DomainBuilderDatabase.getApplicationDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<ApplicationDb> dbList = Arrays.asList(db1, db2);

        Application domain1 = DomainBuilderDatabase.getApplication(db1);
        Application domain2 = DomainBuilderDatabase.getApplication(db2);
        List<Application> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(ActiveEnum.ACTIVE)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Application> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByActive(ActiveEnum.ACTIVE);
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByJobPostingId_shouldReturnMatchingApplications() {
        // Arrange
        Long jobPostingId = 42L;
        ApplicationDb db1 = DomainBuilderDatabase.getApplicationDb();
        List<ApplicationDb> dbList = List.of(db1);

        Application domain1 = DomainBuilderDatabase.getApplication(db1);
        List<Application> domainList = List.of(domain1);

        when(repository.findByJobPostingId(jobPostingId)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Application> result = service.findByJobPostingId(jobPostingId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository).findByJobPostingId(jobPostingId);
        verify(mapper).toModelList(dbList);
    }
}
