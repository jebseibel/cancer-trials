package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.SavedTrialMatchCriterion;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.entity.SavedTrialMatchCriterionDb;
import com.seibel.cancer.database.db.mapper.SavedTrialMatchCriterionMapper;
import com.seibel.cancer.database.db.repository.SavedTrialMatchCriterionRepository;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class SavedTrialMatchCriterionDbServiceTest {

    @Mock
    private SavedTrialMatchCriterionRepository repository;

    @Mock
    private SavedTrialMatchCriterionMapper mapper;

    @InjectMocks
    private SavedTrialMatchCriterionDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        SavedTrialMatchCriterion input = DomainBuilderDatabase.getSavedTrialMatchCriterion();
        SavedTrialMatchCriterionDb mapped = DomainBuilderDatabase.getSavedTrialMatchCriterionDb();

        when(mapper.toDb(input)).thenReturn(mapped);
        when(repository.save(any(SavedTrialMatchCriterionDb.class))).thenReturn(mapped);
        when(mapper.toModel(mapped)).thenReturn(input);

        service.create(input);

        ArgumentCaptor<SavedTrialMatchCriterionDb> captor = ArgumentCaptor.forClass(SavedTrialMatchCriterionDb.class);
        verify(repository).save(captor.capture());

        SavedTrialMatchCriterionDb saved = captor.getValue();
        assertNotNull(saved.getExtid());
        assertEquals(ActiveEnum.ACTIVE, saved.getActive());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void create_positionalOverload_shouldBuildAndPersist() {
        SavedTrialMatchCriterionDb mapped = DomainBuilderDatabase.getSavedTrialMatchCriterionDb();

        when(mapper.toDb(any(SavedTrialMatchCriterion.class))).thenReturn(mapped);
        when(repository.save(any(SavedTrialMatchCriterionDb.class))).thenReturn(mapped);
        when(mapper.toModel(mapped)).thenReturn(DomainBuilderDatabase.getSavedTrialMatchCriterion());

        service.create(42L, "ER positive HER2 negative", new BigDecimal("0.7170"), false, "eligibility", 3);

        ArgumentCaptor<SavedTrialMatchCriterion> captor = ArgumentCaptor.forClass(SavedTrialMatchCriterion.class);
        verify(mapper).toDb(captor.capture());

        SavedTrialMatchCriterion built = captor.getValue();
        assertEquals(42L, built.getTrialMatchId());
        assertEquals("ER positive HER2 negative", built.getChunkText());
        assertEquals(0, new BigDecimal("0.7170").compareTo(built.getScore()));
        assertEquals(false, built.getIsExclusion());
        assertEquals("eligibility", built.getSource());
        assertEquals(3, built.getOrdinal());
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        SavedTrialMatchCriterion input = DomainBuilderDatabase.getSavedTrialMatchCriterion();

        when(mapper.toDb(input)).thenReturn(DomainBuilderDatabase.getSavedTrialMatchCriterionDb());
        when(repository.save(any(SavedTrialMatchCriterionDb.class))).thenThrow(new RuntimeException("boom"));

        assertThrows(ServiceException.class, () -> service.create(input));
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        SavedTrialMatchCriterionDb existing = DomainBuilderDatabase.getSavedTrialMatchCriterionDb();
        SavedTrialMatchCriterion input = DomainBuilderDatabase.getSavedTrialMatchCriterion();

        when(repository.findByExtid(existing.getExtid())).thenReturn(Optional.of(existing));
        when(repository.save(any(SavedTrialMatchCriterionDb.class))).thenReturn(existing);
        when(mapper.toModel(existing)).thenReturn(input);

        service.update(existing.getExtid(), input);

        ArgumentCaptor<SavedTrialMatchCriterionDb> captor = ArgumentCaptor.forClass(SavedTrialMatchCriterionDb.class);
        verify(repository).save(captor.capture());

        SavedTrialMatchCriterionDb saved = captor.getValue();
        assertEquals(input.getChunkText(), saved.getChunkText());
        assertEquals(input.getScore(), saved.getScore());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        SavedTrialMatchCriterion input = DomainBuilderDatabase.getSavedTrialMatchCriterion();

        when(repository.findByExtid(anyString())).thenReturn(Optional.empty());

        assertThrows(ServiceException.class,
                () -> service.update(UUID.randomUUID().toString(), input));
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        SavedTrialMatchCriterionDb existing = DomainBuilderDatabase.getSavedTrialMatchCriterionDb();
        SavedTrialMatchCriterion input = DomainBuilderDatabase.getSavedTrialMatchCriterion();

        when(repository.findByExtid(existing.getExtid())).thenReturn(Optional.of(existing));
        when(repository.save(any(SavedTrialMatchCriterionDb.class))).thenThrow(new RuntimeException("boom"));

        assertThrows(ServiceException.class, () -> service.update(existing.getExtid(), input));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        SavedTrialMatchCriterionDb existing = DomainBuilderDatabase.getSavedTrialMatchCriterionDb();

        when(repository.findByExtid(existing.getExtid())).thenReturn(Optional.of(existing));
        when(repository.save(any(SavedTrialMatchCriterionDb.class))).thenReturn(existing);

        assertTrue(service.delete(existing.getExtid()));

        ArgumentCaptor<SavedTrialMatchCriterionDb> captor = ArgumentCaptor.forClass(SavedTrialMatchCriterionDb.class);
        verify(repository).save(captor.capture());

        SavedTrialMatchCriterionDb saved = captor.getValue();
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
        SavedTrialMatchCriterionDb existing = DomainBuilderDatabase.getSavedTrialMatchCriterionDb();
        SavedTrialMatchCriterion expected = DomainBuilderDatabase.getSavedTrialMatchCriterion(existing);

        when(repository.findByExtid(existing.getExtid())).thenReturn(Optional.of(existing));
        when(mapper.toModel(existing)).thenReturn(expected);

        SavedTrialMatchCriterion found = service.findByExtid(existing.getExtid());

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
        List<SavedTrialMatchCriterionDb> records = List.of(
                DomainBuilderDatabase.getSavedTrialMatchCriterionDb(),
                DomainBuilderDatabase.getSavedTrialMatchCriterionDb());

        when(repository.findAllActive()).thenReturn(records);
        when(mapper.toModelList(records)).thenReturn(List.of(
                DomainBuilderDatabase.getSavedTrialMatchCriterion(),
                DomainBuilderDatabase.getSavedTrialMatchCriterion()));

        assertEquals(2, service.findAll().size());
    }

    @Test
    void findByActive_shouldReturnFilteredEntities() {
        List<SavedTrialMatchCriterionDb> records = List.of(DomainBuilderDatabase.getSavedTrialMatchCriterionDb());

        when(repository.findByActive(ActiveEnum.ACTIVE)).thenReturn(records);
        when(mapper.toModelList(records)).thenReturn(List.of(DomainBuilderDatabase.getSavedTrialMatchCriterion()));

        assertEquals(1, service.findByActive(ActiveEnum.ACTIVE).size());
    }

    @Test
    void findByTrialMatchId_shouldReturnEvidenceForThatMatch() {
        List<SavedTrialMatchCriterionDb> records = List.of(
                DomainBuilderDatabase.getSavedTrialMatchCriterionDb(555L, null),
                DomainBuilderDatabase.getSavedTrialMatchCriterionDb(555L, null));

        when(repository.findByTrialMatchId(555L)).thenReturn(records);
        when(mapper.toModelList(records)).thenReturn(List.of(
                DomainBuilderDatabase.getSavedTrialMatchCriterion(),
                DomainBuilderDatabase.getSavedTrialMatchCriterion()));

        assertEquals(2, service.findByTrialMatchId(555L).size());
    }
}
