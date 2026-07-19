package com.seibel.jobs.database.db.service;

import com.seibel.jobs.common.domain.JobPosting;
import com.seibel.jobs.common.enums.ActiveEnum;
import com.seibel.jobs.common.enums.JobPostingStatus;
import com.seibel.jobs.common.enums.JobSource;
import com.seibel.jobs.common.enums.WorkMode;
import com.seibel.jobs.common.exceptions.ServiceException;
import com.seibel.jobs.database.db.entity.JobPostingDb;
import com.seibel.jobs.database.db.mapper.JobPostingMapper;
import com.seibel.jobs.database.db.repository.JobPostingRepository;
import com.seibel.jobs.testutils.DomainBuilderDatabase;
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
class JobPostingDbServiceTest {

    @Mock
    private JobPostingRepository repository;

    @Mock
    private JobPostingMapper mapper;

    @InjectMocks
    private JobPostingDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        String title = "Backend Engineer";
        Long companyId = 1L;
        String sourceUrl = "https://example.com/jobs/123";
        JobPostingDb savedDb = DomainBuilderDatabase.getJobPostingDb(title, companyId, sourceUrl, null);
        JobPosting expectedDomain = DomainBuilderDatabase.getJobPosting(savedDb);

        when(repository.save(any(JobPostingDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        JobPosting result = service.create(title, companyId, "Description", "Springfield", "IL", "USA",
                WorkMode.REMOTE, 80000, 120000, "USD",
                JobSource.MANUAL, sourceUrl, null, JobPostingStatus.NEW, null);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<JobPostingDb> captor = ArgumentCaptor.forClass(JobPostingDb.class);
        verify(repository).save(captor.capture());

        JobPostingDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(title, captured.getTitle());
        assertEquals(companyId, captured.getCompanyId());
        assertEquals(sourceUrl, captured.getSourceUrl());
        assertEquals(JobSource.MANUAL, captured.getSource());
        assertEquals(JobPostingStatus.NEW, captured.getStatus());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        // Arrange
        when(repository.save(any(JobPostingDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> {
            service.create("Title", 1L, "Description", "City", "State", "Country",
                    WorkMode.REMOTE, 80000, 120000, "USD",
                    JobSource.MANUAL, "https://example.com/jobs/1", null, JobPostingStatus.NEW, null);
        });
        verify(repository).save(any(JobPostingDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        String title = "Updated Title";
        Long companyId = 2L;
        String sourceUrl = "https://example.com/jobs/updated";

        JobPostingDb existingDb = DomainBuilderDatabase.getJobPostingDb("Old Title", 1L, "https://example.com/jobs/old", extid);
        JobPostingDb updatedDb = DomainBuilderDatabase.getJobPostingDb(title, companyId, sourceUrl, extid);
        JobPosting expectedDomain = DomainBuilderDatabase.getJobPosting(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(JobPostingDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        JobPosting result = service.update(extid, title, companyId, "Description", "City", "State", "Country",
                WorkMode.HYBRID, 90000, 130000, "USD",
                JobSource.LINKEDIN, sourceUrl, null, JobPostingStatus.INTERESTED, null);

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<JobPostingDb> captor = ArgumentCaptor.forClass(JobPostingDb.class);
        verify(repository).save(captor.capture());

        JobPostingDb captured = captor.getValue();
        assertEquals(title, captured.getTitle());
        assertEquals(companyId, captured.getCompanyId());
        assertEquals(sourceUrl, captured.getSourceUrl());
        assertEquals(JobSource.LINKEDIN, captured.getSource());
        assertEquals(JobPostingStatus.INTERESTED, captured.getStatus());
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
            service.update(extid, "Title", 1L, "Description", "City", "State", "Country",
                    WorkMode.REMOTE, 80000, 120000, "USD",
                    JobSource.MANUAL, "https://example.com/jobs/1", null, JobPostingStatus.NEW, null);
        });
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        // Arrange
        String extid = "existing-extid";
        JobPostingDb existingDb = DomainBuilderDatabase.getJobPostingDb("Title", 1L, "https://example.com/jobs/1", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(JobPostingDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> {
            service.update(extid, "New Title", 2L, "New Description", "New City", "New State", "New Country",
                    WorkMode.ONSITE, 90000, 130000, "USD",
                    JobSource.INDEED, "https://example.com/jobs/2", null, JobPostingStatus.ARCHIVED, null);
        });
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        JobPostingDb existingDb = DomainBuilderDatabase.getJobPostingDb("Title", 1L, "https://example.com/jobs/1", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(JobPostingDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<JobPostingDb> captor = ArgumentCaptor.forClass(JobPostingDb.class);
        verify(repository).save(captor.capture());

        JobPostingDb captured = captor.getValue();
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
    void findByExtid_shouldReturnJobPosting_whenExists() {
        // Arrange
        String extid = "existing-extid";
        JobPostingDb db = DomainBuilderDatabase.getJobPostingDb("Title", 1L, "https://example.com/jobs/1", extid);
        JobPosting expectedDomain = DomainBuilderDatabase.getJobPosting(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        JobPosting result = service.findByExtid(extid);

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
    void findAll_shouldReturnAllJobPostings() {
        // Arrange
        JobPostingDb db1 = DomainBuilderDatabase.getJobPostingDb();
        JobPostingDb db2 = DomainBuilderDatabase.getJobPostingDb();
        List<JobPostingDb> dbList = Arrays.asList(db1, db2);

        JobPosting domain1 = DomainBuilderDatabase.getJobPosting(db1);
        JobPosting domain2 = DomainBuilderDatabase.getJobPosting(db2);
        List<JobPosting> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<JobPosting> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredJobPostings() {
        // Arrange
        JobPostingDb db1 = DomainBuilderDatabase.getJobPostingDb();
        db1.setActive(ActiveEnum.ACTIVE);
        JobPostingDb db2 = DomainBuilderDatabase.getJobPostingDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<JobPostingDb> dbList = Arrays.asList(db1, db2);

        JobPosting domain1 = DomainBuilderDatabase.getJobPosting(db1);
        JobPosting domain2 = DomainBuilderDatabase.getJobPosting(db2);
        List<JobPosting> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(ActiveEnum.ACTIVE)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<JobPosting> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByActive(ActiveEnum.ACTIVE);
        verify(mapper).toModelList(dbList);
    }
}
