package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.PatientVariant;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.entity.PatientVariantDb;
import com.seibel.cancer.database.db.mapper.PatientVariantMapper;
import com.seibel.cancer.database.db.repository.PatientVariantRepository;
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
class PatientVariantDbServiceTest {

    @Mock
    private PatientVariantRepository repository;

    @Mock
    private PatientVariantMapper mapper;

    @InjectMocks
    private PatientVariantDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        PatientVariant input = DomainBuilderDatabase.getPatientVariant();
        PatientVariantDb mapped = DomainBuilderDatabase.getPatientVariantDb();

        when(mapper.toDb(input)).thenReturn(mapped);
        when(repository.save(any(PatientVariantDb.class))).thenReturn(mapped);
        when(mapper.toModel(mapped)).thenReturn(input);

        service.create(input);

        ArgumentCaptor<PatientVariantDb> captor = ArgumentCaptor.forClass(PatientVariantDb.class);
        verify(repository).save(captor.capture());

        PatientVariantDb saved = captor.getValue();
        assertNotNull(saved.getExtid());
        assertEquals(ActiveEnum.ACTIVE, saved.getActive());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        PatientVariant input = DomainBuilderDatabase.getPatientVariant();

        when(mapper.toDb(input)).thenReturn(DomainBuilderDatabase.getPatientVariantDb());
        when(repository.save(any(PatientVariantDb.class))).thenThrow(new RuntimeException("boom"));

        assertThrows(ServiceException.class, () -> service.create(input));
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        PatientVariantDb existing = DomainBuilderDatabase.getPatientVariantDb();
        PatientVariant input = DomainBuilderDatabase.getPatientVariant();

        when(repository.findByExtid(existing.getExtid())).thenReturn(Optional.of(existing));
        when(repository.save(any(PatientVariantDb.class))).thenReturn(existing);
        when(mapper.toModel(existing)).thenReturn(input);

        service.update(existing.getExtid(), input);

        ArgumentCaptor<PatientVariantDb> captor = ArgumentCaptor.forClass(PatientVariantDb.class);
        verify(repository).save(captor.capture());

        PatientVariantDb saved = captor.getValue();
        assertEquals(input.getPik3caStatus(), saved.getPik3caStatus());
        assertEquals(input.getBrca1Status(), saved.getBrca1Status());
        assertEquals(input.getKi67Percent(), saved.getKi67Percent());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        PatientVariant input = DomainBuilderDatabase.getPatientVariant();

        when(repository.findByExtid(anyString())).thenReturn(Optional.empty());

        assertThrows(ServiceException.class, () -> service.update(UUID.randomUUID().toString(), input));
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        PatientVariantDb existing = DomainBuilderDatabase.getPatientVariantDb();
        PatientVariant input = DomainBuilderDatabase.getPatientVariant();

        when(repository.findByExtid(existing.getExtid())).thenReturn(Optional.of(existing));
        when(repository.save(any(PatientVariantDb.class))).thenThrow(new RuntimeException("boom"));

        assertThrows(ServiceException.class, () -> service.update(existing.getExtid(), input));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        PatientVariantDb existing = DomainBuilderDatabase.getPatientVariantDb();

        when(repository.findByExtid(existing.getExtid())).thenReturn(Optional.of(existing));
        when(repository.save(any(PatientVariantDb.class))).thenReturn(existing);

        assertTrue(service.delete(existing.getExtid()));

        ArgumentCaptor<PatientVariantDb> captor = ArgumentCaptor.forClass(PatientVariantDb.class);
        verify(repository).save(captor.capture());

        PatientVariantDb saved = captor.getValue();
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
        PatientVariantDb existing = DomainBuilderDatabase.getPatientVariantDb();
        PatientVariant expected = DomainBuilderDatabase.getPatientVariant(existing);

        when(repository.findByExtid(existing.getExtid())).thenReturn(Optional.of(existing));
        when(mapper.toModel(existing)).thenReturn(expected);

        PatientVariant found = service.findByExtid(existing.getExtid());

        assertEquals(expected.getExtid(), found.getExtid());
    }

    @Test
    void findByExtid_shouldThrowException_whenNotFound() {
        when(repository.findByExtid(anyString())).thenReturn(Optional.empty());

        assertThrows(ServiceException.class, () -> service.findByExtid(UUID.randomUUID().toString()));
    }

    @Test
    void findAll_shouldReturnAllEntities() {
        List<PatientVariantDb> records = List.of(
                DomainBuilderDatabase.getPatientVariantDb(),
                DomainBuilderDatabase.getPatientVariantDb());

        when(repository.findAllActive()).thenReturn(records);
        when(mapper.toModelList(records)).thenReturn(List.of(
                DomainBuilderDatabase.getPatientVariant(),
                DomainBuilderDatabase.getPatientVariant()));

        assertEquals(2, service.findAll().size());
    }

    @Test
    void findByActive_shouldReturnFilteredEntities() {
        List<PatientVariantDb> records = List.of(DomainBuilderDatabase.getPatientVariantDb());

        when(repository.findByActive(ActiveEnum.ACTIVE)).thenReturn(records);
        when(mapper.toModelList(records)).thenReturn(List.of(DomainBuilderDatabase.getPatientVariant()));

        assertEquals(1, service.findByActive(ActiveEnum.ACTIVE).size());
    }

    @Test
    void findByAppUserId_shouldReturnPatientRows() {
        List<PatientVariantDb> records = List.of(DomainBuilderDatabase.getPatientVariantDb(4242L, "DETECTED"));

        when(repository.findByAppUserId(4242L)).thenReturn(records);
        when(mapper.toModelList(records)).thenReturn(List.of(DomainBuilderDatabase.getPatientVariant(records.get(0))));

        List<PatientVariant> found = service.findByAppUserId(4242L);

        assertEquals(1, found.size());
        assertEquals("DETECTED", found.get(0).getPik3caStatus());
    }
}
