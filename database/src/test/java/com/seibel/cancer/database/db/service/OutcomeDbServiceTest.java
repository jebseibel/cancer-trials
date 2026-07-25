package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.Outcome;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.OutcomeDb;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.mapper.OutcomeMapper;
import com.seibel.cancer.database.db.repository.OutcomeRepository;
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
class OutcomeDbServiceTest {

    @Mock
    private OutcomeRepository repository;

    @Mock
    private OutcomeMapper mapper;

    @InjectMocks
    private OutcomeDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        Outcome input = DomainBuilderDatabase.getOutcome();
        OutcomeDb savedDb = DomainBuilderDatabase.getOutcomeDb(input.getTrialId(), input.getOutcomeType());
        Outcome expectedDomain = DomainBuilderDatabase.getOutcome(savedDb);

        when(mapper.toDb(input)).thenReturn(new OutcomeDb());
        when(repository.save(any(OutcomeDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        Outcome result = service.create(input);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<OutcomeDb> captor = ArgumentCaptor.forClass(OutcomeDb.class);
        verify(repository).save(captor.capture());

        OutcomeDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        // Arrange
        Outcome input = DomainBuilderDatabase.getOutcome();
        when(mapper.toDb(input)).thenReturn(new OutcomeDb());
        when(repository.save(any(OutcomeDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.create(input));
        verify(repository).save(any(OutcomeDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void createPositional_shouldGenerateUuidAndSetFields() {
        // Arrange
        Long trialId = 42L;
        String outcomeType = "PRIMARY";
        String measure = "Overall survival";
        String description = "Time from randomization to death";
        String timeFrame = "24 months";

        OutcomeDb savedDb = DomainBuilderDatabase.getOutcomeDb(trialId, outcomeType, measure, null);
        Outcome expectedDomain = DomainBuilderDatabase.getOutcome(savedDb);

        when(repository.save(any(OutcomeDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        Outcome result = service.create(trialId, outcomeType, measure, description, timeFrame);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<OutcomeDb> captor = ArgumentCaptor.forClass(OutcomeDb.class);
        verify(repository).save(captor.capture());

        OutcomeDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(trialId, captured.getTrialId());
        assertEquals(outcomeType, captured.getOutcomeType());
        assertEquals(measure, captured.getMeasure());
        assertEquals(description, captured.getDescription());
        assertEquals(timeFrame, captured.getTimeFrame());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void createPositional_shouldThrowException_whenRepositoryFails() {
        // Arrange
        when(repository.save(any(OutcomeDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () ->
                service.create(1L, "PRIMARY", "Measure", "Description", "Time frame"));
        verify(repository).save(any(OutcomeDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        OutcomeDb existingDb = DomainBuilderDatabase.getOutcomeDb(1L, "OLD_TYPE", "Old Measure", extid);

        OutcomeDb updatedDb = DomainBuilderDatabase.getOutcomeDb(1L, "NEW_TYPE", "New Measure", extid);
        Outcome expectedDomain = DomainBuilderDatabase.getOutcome(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(OutcomeDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        Outcome result = service.update(extid, null, "NEW_TYPE", "New Measure", null, null);

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<OutcomeDb> captor = ArgumentCaptor.forClass(OutcomeDb.class);
        verify(repository).save(captor.capture());

        OutcomeDb captured = captor.getValue();
        assertEquals("NEW_TYPE", captured.getOutcomeType());
        assertEquals("New Measure", captured.getMeasure());
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
                service.update(extid, null, "TYPE", "Measure", null, null));
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        // Arrange
        String extid = "existing-extid";
        OutcomeDb existingDb = DomainBuilderDatabase.getOutcomeDb(1L, "TYPE", "Measure", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(OutcomeDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () ->
                service.update(extid, null, "NEW_TYPE", null, null, null));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        OutcomeDb existingDb = DomainBuilderDatabase.getOutcomeDb(1L, "TYPE", "Measure", extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(OutcomeDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<OutcomeDb> captor = ArgumentCaptor.forClass(OutcomeDb.class);
        verify(repository).save(captor.capture());

        OutcomeDb captured = captor.getValue();
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
        OutcomeDb db = DomainBuilderDatabase.getOutcomeDb(1L, "TYPE", "Measure", extid);
        Outcome expectedDomain = DomainBuilderDatabase.getOutcome(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        Outcome result = service.findByExtid(extid);

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
        OutcomeDb db1 = DomainBuilderDatabase.getOutcomeDb();
        OutcomeDb db2 = DomainBuilderDatabase.getOutcomeDb();
        List<OutcomeDb> dbList = Arrays.asList(db1, db2);

        Outcome domain1 = DomainBuilderDatabase.getOutcome(db1);
        Outcome domain2 = DomainBuilderDatabase.getOutcome(db2);
        List<Outcome> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Outcome> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredEntities() {
        // Arrange
        OutcomeDb db1 = DomainBuilderDatabase.getOutcomeDb();
        db1.setActive(ActiveEnum.ACTIVE);
        OutcomeDb db2 = DomainBuilderDatabase.getOutcomeDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<OutcomeDb> dbList = Arrays.asList(db1, db2);

        Outcome domain1 = DomainBuilderDatabase.getOutcome(db1);
        Outcome domain2 = DomainBuilderDatabase.getOutcome(db2);
        List<Outcome> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(any(ActiveEnum.class))).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Outcome> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByActive(any(ActiveEnum.class));
        verify(mapper).toModelList(dbList);
    }
}
