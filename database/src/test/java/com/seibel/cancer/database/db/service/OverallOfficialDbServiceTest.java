package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.OverallOfficial;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.OverallOfficialDb;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.mapper.OverallOfficialMapper;
import com.seibel.cancer.database.db.repository.OverallOfficialRepository;
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
class OverallOfficialDbServiceTest {

    @Mock
    private OverallOfficialRepository repository;

    @Mock
    private OverallOfficialMapper mapper;

    @InjectMocks
    private OverallOfficialDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        OverallOfficial input = DomainBuilderDatabase.getOverallOfficial();
        OverallOfficialDb savedDb = DomainBuilderDatabase.getOverallOfficialDb(input.getTrialId(), input.getName());
        OverallOfficial expectedDomain = DomainBuilderDatabase.getOverallOfficial(savedDb);

        when(mapper.toDb(input)).thenReturn(new OverallOfficialDb());
        when(repository.save(any(OverallOfficialDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        OverallOfficial result = service.create(input);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<OverallOfficialDb> captor = ArgumentCaptor.forClass(OverallOfficialDb.class);
        verify(repository).save(captor.capture());

        OverallOfficialDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        // Arrange
        OverallOfficial input = DomainBuilderDatabase.getOverallOfficial();
        when(mapper.toDb(input)).thenReturn(new OverallOfficialDb());
        when(repository.save(any(OverallOfficialDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.create(input));
        verify(repository).save(any(OverallOfficialDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void create_positional_shouldGenerateUuidAndSetFields() {
        // Arrange
        Long trialId = 42L;
        String name = "Dr. Jane Smith";
        String affiliation = "Mayo Clinic";
        String role = "Principal Investigator";

        OverallOfficialDb savedDb = DomainBuilderDatabase.getOverallOfficialDb(trialId, name);
        OverallOfficial expectedDomain = DomainBuilderDatabase.getOverallOfficial(savedDb);

        when(repository.save(any(OverallOfficialDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        OverallOfficial result = service.create(trialId, name, affiliation, role);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<OverallOfficialDb> captor = ArgumentCaptor.forClass(OverallOfficialDb.class);
        verify(repository).save(captor.capture());

        OverallOfficialDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(trialId, captured.getTrialId());
        assertEquals(name, captured.getName());
        assertEquals(affiliation, captured.getAffiliation());
        assertEquals(role, captured.getRole());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void create_positional_shouldThrowException_whenRepositoryFails() {
        // Arrange
        when(repository.save(any(OverallOfficialDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.create(42L, "Dr. Jane Smith", "Mayo Clinic", "PI"));
        verify(repository).save(any(OverallOfficialDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        OverallOfficialDb existingDb = DomainBuilderDatabase.getOverallOfficialDb(1L, "Old Name", "Old Affiliation", "Old Role", extid);

        OverallOfficialDb updatedDb = DomainBuilderDatabase.getOverallOfficialDb(2L, "Updated Name", "Updated Affiliation", "Updated Role", extid);
        OverallOfficial expectedDomain = DomainBuilderDatabase.getOverallOfficial(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(OverallOfficialDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        OverallOfficial result = service.update(extid, 2L, "Updated Name", "Updated Affiliation", "Updated Role");

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<OverallOfficialDb> captor = ArgumentCaptor.forClass(OverallOfficialDb.class);
        verify(repository).save(captor.capture());

        OverallOfficialDb captured = captor.getValue();
        assertEquals(2L, captured.getTrialId());
        assertEquals("Updated Name", captured.getName());
        assertEquals("Updated Affiliation", captured.getAffiliation());
        assertEquals("Updated Role", captured.getRole());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(updatedDb);
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        // Arrange
        String extid = "nonexistent-extid";
        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.update(extid, null, "Doesn't matter", null, null));
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        // Arrange
        String extid = "existing-extid";
        OverallOfficialDb existingDb = DomainBuilderDatabase.getOverallOfficialDb(1L, "Old Name", null, null, extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(OverallOfficialDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.update(extid, null, "New Name", null, null));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        OverallOfficialDb existingDb = DomainBuilderDatabase.getOverallOfficialDb(1L, "Name", null, null, extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(OverallOfficialDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<OverallOfficialDb> captor = ArgumentCaptor.forClass(OverallOfficialDb.class);
        verify(repository).save(captor.capture());

        OverallOfficialDb captured = captor.getValue();
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
    void findByExtid_shouldReturnEntity_whenExists() {
        // Arrange
        String extid = "existing-extid";
        OverallOfficialDb db = DomainBuilderDatabase.getOverallOfficialDb(1L, "Name", null, null, extid);
        OverallOfficial expectedDomain = DomainBuilderDatabase.getOverallOfficial(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        OverallOfficial result = service.findByExtid(extid);

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
        OverallOfficialDb db1 = DomainBuilderDatabase.getOverallOfficialDb();
        OverallOfficialDb db2 = DomainBuilderDatabase.getOverallOfficialDb();
        List<OverallOfficialDb> dbList = Arrays.asList(db1, db2);

        OverallOfficial domain1 = DomainBuilderDatabase.getOverallOfficial(db1);
        OverallOfficial domain2 = DomainBuilderDatabase.getOverallOfficial(db2);
        List<OverallOfficial> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<OverallOfficial> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredEntities() {
        // Arrange
        OverallOfficialDb db1 = DomainBuilderDatabase.getOverallOfficialDb();
        db1.setActive(ActiveEnum.ACTIVE);
        OverallOfficialDb db2 = DomainBuilderDatabase.getOverallOfficialDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<OverallOfficialDb> dbList = Arrays.asList(db1, db2);

        OverallOfficial domain1 = DomainBuilderDatabase.getOverallOfficial(db1);
        OverallOfficial domain2 = DomainBuilderDatabase.getOverallOfficial(db2);
        List<OverallOfficial> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(any(ActiveEnum.class))).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<OverallOfficial> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByActive(any(ActiveEnum.class));
        verify(mapper).toModelList(dbList);
    }
}
