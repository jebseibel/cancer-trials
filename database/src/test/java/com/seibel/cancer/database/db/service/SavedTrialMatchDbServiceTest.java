package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.SavedTrialMatch;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.entity.SavedTrialMatchDb;
import com.seibel.cancer.database.db.mapper.SavedTrialMatchMapper;
import com.seibel.cancer.database.db.repository.SavedTrialMatchRepository;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrialMatchDbServiceTest {

    @Mock
    private SavedTrialMatchRepository repository;

    @Mock
    private SavedTrialMatchMapper mapper;

    @InjectMocks
    private SavedTrialMatchDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        SavedTrialMatch input = DomainBuilderDatabase.getSavedTrialMatch();
        SavedTrialMatchDb mapped = DomainBuilderDatabase.getSavedTrialMatchDb();

        when(mapper.toDb(input)).thenReturn(mapped);
        when(repository.save(any(SavedTrialMatchDb.class))).thenReturn(mapped);
        when(mapper.toModel(mapped)).thenReturn(input);

        service.create(input);

        ArgumentCaptor<SavedTrialMatchDb> captor = ArgumentCaptor.forClass(SavedTrialMatchDb.class);
        verify(repository).save(captor.capture());

        SavedTrialMatchDb saved = captor.getValue();
        assertNotNull(saved.getExtid());
        assertEquals(ActiveEnum.ACTIVE, saved.getActive());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        SavedTrialMatch input = DomainBuilderDatabase.getSavedTrialMatch();

        when(mapper.toDb(input)).thenReturn(DomainBuilderDatabase.getSavedTrialMatchDb());
        when(repository.save(any(SavedTrialMatchDb.class))).thenThrow(new RuntimeException("boom"));

        assertThrows(ServiceException.class, () -> service.create(input));
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        SavedTrialMatchDb existing = DomainBuilderDatabase.getSavedTrialMatchDb();
        SavedTrialMatch input = DomainBuilderDatabase.getSavedTrialMatch();

        when(repository.findByExtid(existing.getExtid())).thenReturn(Optional.of(existing));
        when(repository.save(any(SavedTrialMatchDb.class))).thenReturn(existing);
        when(mapper.toModel(existing)).thenReturn(input);

        service.update(existing.getExtid(), input);

        ArgumentCaptor<SavedTrialMatchDb> captor = ArgumentCaptor.forClass(SavedTrialMatchDb.class);
        verify(repository).save(captor.capture());

        SavedTrialMatchDb saved = captor.getValue();
        assertEquals(input.getSearchRunId(), saved.getSearchRunId());
        assertEquals(input.getTopScore(), saved.getTopScore());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        SavedTrialMatch input = DomainBuilderDatabase.getSavedTrialMatch();

        when(repository.findByExtid(anyString())).thenReturn(Optional.empty());

        assertThrows(ServiceException.class,
                () -> service.update(UUID.randomUUID().toString(), input));
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        SavedTrialMatchDb existing = DomainBuilderDatabase.getSavedTrialMatchDb();
        SavedTrialMatch input = DomainBuilderDatabase.getSavedTrialMatch();

        when(repository.findByExtid(existing.getExtid())).thenReturn(Optional.of(existing));
        when(repository.save(any(SavedTrialMatchDb.class))).thenThrow(new RuntimeException("boom"));

        assertThrows(ServiceException.class, () -> service.update(existing.getExtid(), input));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        SavedTrialMatchDb existing = DomainBuilderDatabase.getSavedTrialMatchDb();

        when(repository.findByExtid(existing.getExtid())).thenReturn(Optional.of(existing));
        when(repository.save(any(SavedTrialMatchDb.class))).thenReturn(existing);

        assertTrue(service.delete(existing.getExtid()));

        ArgumentCaptor<SavedTrialMatchDb> captor = ArgumentCaptor.forClass(SavedTrialMatchDb.class);
        verify(repository).save(captor.capture());

        SavedTrialMatchDb saved = captor.getValue();
        assertNotNull(saved.getDeletedAt());
        assertEquals(ActiveEnum.INACTIVE, saved.getActive());
    }

    @Test
    void delete_shouldThrowException_whenNotFound() {
        when(repository.findByExtid(anyString())).thenReturn(Optional.empty());

        assertThrows(ServiceException.class, () -> service.delete(UUID.randomUUID().toString()));
    }

    @Test
    void findByExtid_shouldReturnEntity_whenExists() {
        SavedTrialMatchDb existing = DomainBuilderDatabase.getSavedTrialMatchDb();
        SavedTrialMatch expected = DomainBuilderDatabase.getSavedTrialMatch(existing);

        when(repository.findByExtid(existing.getExtid())).thenReturn(Optional.of(existing));
        when(mapper.toModel(existing)).thenReturn(expected);

        SavedTrialMatch found = service.findByExtid(existing.getExtid());

        assertNotNull(found);
        assertEquals(expected.getExtid(), found.getExtid());
    }

    @Test
    void findByExtid_shouldThrowException_whenNotFound() {
        when(repository.findByExtid(anyString())).thenReturn(Optional.empty());

        assertThrows(ServiceException.class,
                () -> service.findByExtid(UUID.randomUUID().toString()));
    }

    @Test
    void findAll_shouldReturnAllEntities() {
        List<SavedTrialMatchDb> records = List.of(
                DomainBuilderDatabase.getSavedTrialMatchDb(),
                DomainBuilderDatabase.getSavedTrialMatchDb());

        when(repository.findAllActive()).thenReturn(records);
        when(mapper.toModelList(records)).thenReturn(List.of(
                DomainBuilderDatabase.getSavedTrialMatch(),
                DomainBuilderDatabase.getSavedTrialMatch()));

        assertEquals(2, service.findAll().size());
    }

    @Test
    void findByActive_shouldReturnFilteredEntities() {
        List<SavedTrialMatchDb> records = List.of(DomainBuilderDatabase.getSavedTrialMatchDb());

        when(repository.findByActive(ActiveEnum.ACTIVE)).thenReturn(records);
        when(mapper.toModelList(records)).thenReturn(List.of(DomainBuilderDatabase.getSavedTrialMatch()));

        assertEquals(1, service.findByActive(ActiveEnum.ACTIVE).size());
    }

    @Test
    void findBySearchRunId_shouldReturnMatchesForThatRun() {
        String runId = UUID.randomUUID().toString();
        List<SavedTrialMatchDb> records = List.of(
                DomainBuilderDatabase.getSavedTrialMatchDb(null, runId),
                DomainBuilderDatabase.getSavedTrialMatchDb(null, runId));

        when(repository.findBySearchRunId(runId)).thenReturn(records);
        when(mapper.toModelList(records)).thenReturn(List.of(
                DomainBuilderDatabase.getSavedTrialMatch(),
                DomainBuilderDatabase.getSavedTrialMatch()));

        assertEquals(2, service.findBySearchRunId(runId).size());
    }

    @Test
    void findByPatientId_shouldReturnEntities() {
        List<SavedTrialMatchDb> records = List.of(DomainBuilderDatabase.getSavedTrialMatchDb());

        when(repository.findByPatientId(4242L)).thenReturn(records);
        when(mapper.toModelList(records)).thenReturn(List.of(DomainBuilderDatabase.getSavedTrialMatch()));

        assertEquals(1, service.findByPatientId(4242L).size());
    }

    @Test
    void findByTrialId_shouldReturnEntities() {
        List<SavedTrialMatchDb> records = List.of(DomainBuilderDatabase.getSavedTrialMatchDb());

        when(repository.findByTrialId(777L)).thenReturn(records);
        when(mapper.toModelList(records)).thenReturn(List.of(DomainBuilderDatabase.getSavedTrialMatch()));

        assertEquals(1, service.findByTrialId(777L).size());
    }
}
