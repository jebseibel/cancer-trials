package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.Sponsor;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.SponsorDb;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.mapper.SponsorMapper;
import com.seibel.cancer.database.db.repository.SponsorRepository;
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
class SponsorDbServiceTest {

    @Mock
    private SponsorRepository repository;

    @Mock
    private SponsorMapper mapper;

    @InjectMocks
    private SponsorDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        Sponsor input = DomainBuilderDatabase.getSponsor();
        SponsorDb savedDb = DomainBuilderDatabase.getSponsorDb(input.getName(), input.getOrgClass());
        Sponsor expectedDomain = DomainBuilderDatabase.getSponsor(savedDb);

        when(mapper.toDb(input)).thenReturn(new SponsorDb());
        when(repository.save(any(SponsorDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        Sponsor result = service.create(input);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<SponsorDb> captor = ArgumentCaptor.forClass(SponsorDb.class);
        verify(repository).save(captor.capture());

        SponsorDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        // Arrange
        Sponsor input = DomainBuilderDatabase.getSponsor();
        when(mapper.toDb(input)).thenReturn(new SponsorDb());
        when(repository.save(any(SponsorDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.create(input));
        verify(repository).save(any(SponsorDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        SponsorDb existingDb = DomainBuilderDatabase.getSponsorDb("Old Name", "Old Class", extid);

        SponsorDb updatedDb = DomainBuilderDatabase.getSponsorDb("New Name", "New Class", extid);
        Sponsor expectedDomain = DomainBuilderDatabase.getSponsor(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(SponsorDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        Sponsor result = service.update(extid, "New Name", "New Class");

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<SponsorDb> captor = ArgumentCaptor.forClass(SponsorDb.class);
        verify(repository).save(captor.capture());

        SponsorDb captured = captor.getValue();
        assertEquals("New Name", captured.getName());
        assertEquals("New Class", captured.getOrgClass());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(updatedDb);
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        // Arrange
        String extid = "nonexistent-extid";
        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.update(extid, "New Name", "New Class"));
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        // Arrange
        String extid = "existing-extid";
        SponsorDb existingDb = DomainBuilderDatabase.getSponsorDb("Name", "Class", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(SponsorDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.update(extid, "New Name", "New Class"));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        SponsorDb existingDb = DomainBuilderDatabase.getSponsorDb("Name", "Class", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(SponsorDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<SponsorDb> captor = ArgumentCaptor.forClass(SponsorDb.class);
        verify(repository).save(captor.capture());

        SponsorDb captured = captor.getValue();
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
    void findByExtid_shouldReturnSponsor_whenExists() {
        // Arrange
        String extid = "existing-extid";
        SponsorDb db = DomainBuilderDatabase.getSponsorDb("Name", "Class", extid);
        Sponsor expectedDomain = DomainBuilderDatabase.getSponsor(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        Sponsor result = service.findByExtid(extid);

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
    void findAll_shouldReturnAllEntities() {
        // Arrange
        SponsorDb db1 = DomainBuilderDatabase.getSponsorDb();
        SponsorDb db2 = DomainBuilderDatabase.getSponsorDb();
        List<SponsorDb> dbList = Arrays.asList(db1, db2);

        Sponsor domain1 = DomainBuilderDatabase.getSponsor(db1);
        Sponsor domain2 = DomainBuilderDatabase.getSponsor(db2);
        List<Sponsor> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Sponsor> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredEntities() {
        // Arrange
        SponsorDb db1 = DomainBuilderDatabase.getSponsorDb();
        db1.setActive(ActiveEnum.ACTIVE);
        SponsorDb db2 = DomainBuilderDatabase.getSponsorDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<SponsorDb> dbList = Arrays.asList(db1, db2);

        Sponsor domain1 = DomainBuilderDatabase.getSponsor(db1);
        Sponsor domain2 = DomainBuilderDatabase.getSponsor(db2);
        List<Sponsor> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(any(ActiveEnum.class))).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Sponsor> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByActive(any(ActiveEnum.class));
        verify(mapper).toModelList(dbList);
    }
}
