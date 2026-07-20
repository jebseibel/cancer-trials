package com.seibel.jobhunting.database.db.service;

import com.seibel.jobhunting.common.domain.JobPostingSkill;
import com.seibel.jobhunting.common.enums.ActiveEnum;
import com.seibel.jobhunting.common.exceptions.ServiceException;
import com.seibel.jobhunting.database.db.entity.JobPostingSkillDb;
import com.seibel.jobhunting.database.db.mapper.JobPostingSkillMapper;
import com.seibel.jobhunting.database.db.repository.JobPostingSkillRepository;
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
class JobPostingSkillDbServiceTest {

    @Mock
    private JobPostingSkillRepository repository;

    @Mock
    private JobPostingSkillMapper mapper;

    @InjectMocks
    private JobPostingSkillDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        Long jobPostingId = 1L;
        Long skillId = 2L;
        JobPostingSkillDb savedDb = DomainBuilderDatabase.getJobPostingSkillDb(jobPostingId, skillId, null);
        JobPostingSkill expectedDomain = DomainBuilderDatabase.getJobPostingSkill(savedDb);

        when(repository.save(any(JobPostingSkillDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        JobPostingSkill result = service.create(jobPostingId, skillId);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<JobPostingSkillDb> captor = ArgumentCaptor.forClass(JobPostingSkillDb.class);
        verify(repository).save(captor.capture());

        JobPostingSkillDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(jobPostingId, captured.getJobPostingId());
        assertEquals(skillId, captured.getSkillId());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        // Arrange
        when(repository.save(any(JobPostingSkillDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.create(1L, 2L));
        verify(repository).save(any(JobPostingSkillDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        Long jobPostingId = 3L;
        Long skillId = 4L;

        JobPostingSkillDb existingDb = DomainBuilderDatabase.getJobPostingSkillDb(1L, 2L, extid);
        JobPostingSkillDb updatedDb = DomainBuilderDatabase.getJobPostingSkillDb(jobPostingId, skillId, extid);
        JobPostingSkill expectedDomain = DomainBuilderDatabase.getJobPostingSkill(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(JobPostingSkillDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        JobPostingSkill result = service.update(extid, jobPostingId, skillId);

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<JobPostingSkillDb> captor = ArgumentCaptor.forClass(JobPostingSkillDb.class);
        verify(repository).save(captor.capture());

        JobPostingSkillDb captured = captor.getValue();
        assertEquals(jobPostingId, captured.getJobPostingId());
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
        JobPostingSkillDb existingDb = DomainBuilderDatabase.getJobPostingSkillDb(1L, 2L, extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(JobPostingSkillDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.update(extid, 3L, 4L));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        JobPostingSkillDb existingDb = DomainBuilderDatabase.getJobPostingSkillDb(1L, 2L, extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(JobPostingSkillDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<JobPostingSkillDb> captor = ArgumentCaptor.forClass(JobPostingSkillDb.class);
        verify(repository).save(captor.capture());

        JobPostingSkillDb captured = captor.getValue();
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
    void findByExtid_shouldReturnJobPostingSkill_whenExists() {
        // Arrange
        String extid = "existing-extid";
        JobPostingSkillDb db = DomainBuilderDatabase.getJobPostingSkillDb(1L, 2L, extid);
        JobPostingSkill expectedDomain = DomainBuilderDatabase.getJobPostingSkill(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        JobPostingSkill result = service.findByExtid(extid);

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
    void findAll_shouldReturnAllJobPostingSkills() {
        // Arrange
        JobPostingSkillDb db1 = DomainBuilderDatabase.getJobPostingSkillDb();
        JobPostingSkillDb db2 = DomainBuilderDatabase.getJobPostingSkillDb();
        List<JobPostingSkillDb> dbList = Arrays.asList(db1, db2);

        JobPostingSkill domain1 = DomainBuilderDatabase.getJobPostingSkill(db1);
        JobPostingSkill domain2 = DomainBuilderDatabase.getJobPostingSkill(db2);
        List<JobPostingSkill> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<JobPostingSkill> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredJobPostingSkills() {
        // Arrange
        JobPostingSkillDb db1 = DomainBuilderDatabase.getJobPostingSkillDb();
        db1.setActive(ActiveEnum.ACTIVE);
        JobPostingSkillDb db2 = DomainBuilderDatabase.getJobPostingSkillDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<JobPostingSkillDb> dbList = Arrays.asList(db1, db2);

        JobPostingSkill domain1 = DomainBuilderDatabase.getJobPostingSkill(db1);
        JobPostingSkill domain2 = DomainBuilderDatabase.getJobPostingSkill(db2);
        List<JobPostingSkill> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(ActiveEnum.ACTIVE)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<JobPostingSkill> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByActive(ActiveEnum.ACTIVE);
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByJobPostingId_shouldReturnMatchingLinks() {
        // Arrange
        Long jobPostingId = 42L;
        JobPostingSkillDb db1 = DomainBuilderDatabase.getJobPostingSkillDb();
        List<JobPostingSkillDb> dbList = List.of(db1);

        JobPostingSkill domain1 = DomainBuilderDatabase.getJobPostingSkill(db1);
        List<JobPostingSkill> domainList = List.of(domain1);

        when(repository.findByJobPostingId(jobPostingId)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<JobPostingSkill> result = service.findByJobPostingId(jobPostingId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository).findByJobPostingId(jobPostingId);
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findBySkillId_shouldReturnMatchingLinks() {
        // Arrange
        Long skillId = 42L;
        JobPostingSkillDb db1 = DomainBuilderDatabase.getJobPostingSkillDb();
        List<JobPostingSkillDb> dbList = List.of(db1);

        JobPostingSkill domain1 = DomainBuilderDatabase.getJobPostingSkill(db1);
        List<JobPostingSkill> domainList = List.of(domain1);

        when(repository.findBySkillId(skillId)).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<JobPostingSkill> result = service.findBySkillId(skillId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(repository).findBySkillId(skillId);
        verify(mapper).toModelList(dbList);
    }
}
