package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.PatientPriorTreatment;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.entity.PatientPriorTreatmentDb;
import com.seibel.cancer.database.db.mapper.PatientPriorTreatmentMapper;
import com.seibel.cancer.database.db.repository.PatientPriorTreatmentRepository;
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
class PatientPriorTreatmentDbServiceTest {

    @Mock
    private PatientPriorTreatmentRepository repository;

    @Mock
    private PatientPriorTreatmentMapper mapper;

    @InjectMocks
    private PatientPriorTreatmentDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        PatientPriorTreatment input = DomainBuilderDatabase.getPatientPriorTreatment();
        PatientPriorTreatmentDb mapped = DomainBuilderDatabase.getPatientPriorTreatmentDb();

        when(mapper.toDb(input)).thenReturn(mapped);
        when(repository.save(any(PatientPriorTreatmentDb.class))).thenReturn(mapped);
        when(mapper.toModel(mapped)).thenReturn(input);

        service.create(input);

        ArgumentCaptor<PatientPriorTreatmentDb> captor = ArgumentCaptor.forClass(PatientPriorTreatmentDb.class);
        verify(repository).save(captor.capture());

        PatientPriorTreatmentDb saved = captor.getValue();
        assertNotNull(saved.getExtid());
        assertEquals(ActiveEnum.ACTIVE, saved.getActive());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        PatientPriorTreatment input = DomainBuilderDatabase.getPatientPriorTreatment();

        when(mapper.toDb(input)).thenReturn(DomainBuilderDatabase.getPatientPriorTreatmentDb());
        when(repository.save(any(PatientPriorTreatmentDb.class))).thenThrow(new RuntimeException("boom"));

        assertThrows(ServiceException.class, () -> service.create(input));
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        PatientPriorTreatmentDb existing = DomainBuilderDatabase.getPatientPriorTreatmentDb();
        PatientPriorTreatment input = DomainBuilderDatabase.getPatientPriorTreatment();

        when(repository.findByExtid(existing.getExtid())).thenReturn(Optional.of(existing));
        when(repository.save(any(PatientPriorTreatmentDb.class))).thenReturn(existing);
        when(mapper.toModel(existing)).thenReturn(input);

        service.update(existing.getExtid(), input);

        ArgumentCaptor<PatientPriorTreatmentDb> captor = ArgumentCaptor.forClass(PatientPriorTreatmentDb.class);
        verify(repository).save(captor.capture());

        PatientPriorTreatmentDb saved = captor.getValue();
        assertEquals(input.getCdk46Status(), saved.getCdk46Status());
        assertEquals(input.getEndocrineStatus(), saved.getEndocrineStatus());
        assertEquals(input.getLinesOfTherapyMetastatic(), saved.getLinesOfTherapyMetastatic());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        PatientPriorTreatment input = DomainBuilderDatabase.getPatientPriorTreatment();

        when(repository.findByExtid(anyString())).thenReturn(Optional.empty());

        assertThrows(ServiceException.class, () -> service.update(UUID.randomUUID().toString(), input));
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        PatientPriorTreatmentDb existing = DomainBuilderDatabase.getPatientPriorTreatmentDb();
        PatientPriorTreatment input = DomainBuilderDatabase.getPatientPriorTreatment();

        when(repository.findByExtid(existing.getExtid())).thenReturn(Optional.of(existing));
        when(repository.save(any(PatientPriorTreatmentDb.class))).thenThrow(new RuntimeException("boom"));

        assertThrows(ServiceException.class, () -> service.update(existing.getExtid(), input));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        PatientPriorTreatmentDb existing = DomainBuilderDatabase.getPatientPriorTreatmentDb();

        when(repository.findByExtid(existing.getExtid())).thenReturn(Optional.of(existing));
        when(repository.save(any(PatientPriorTreatmentDb.class))).thenReturn(existing);

        assertTrue(service.delete(existing.getExtid()));

        ArgumentCaptor<PatientPriorTreatmentDb> captor = ArgumentCaptor.forClass(PatientPriorTreatmentDb.class);
        verify(repository).save(captor.capture());

        PatientPriorTreatmentDb saved = captor.getValue();
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
        PatientPriorTreatmentDb existing = DomainBuilderDatabase.getPatientPriorTreatmentDb();
        PatientPriorTreatment expected = DomainBuilderDatabase.getPatientPriorTreatment(existing);

        when(repository.findByExtid(existing.getExtid())).thenReturn(Optional.of(existing));
        when(mapper.toModel(existing)).thenReturn(expected);

        PatientPriorTreatment found = service.findByExtid(existing.getExtid());

        assertEquals(expected.getExtid(), found.getExtid());
    }

    @Test
    void findByExtid_shouldThrowException_whenNotFound() {
        when(repository.findByExtid(anyString())).thenReturn(Optional.empty());

        assertThrows(ServiceException.class, () -> service.findByExtid(UUID.randomUUID().toString()));
    }

    @Test
    void findAll_shouldReturnAllEntities() {
        List<PatientPriorTreatmentDb> records = List.of(
                DomainBuilderDatabase.getPatientPriorTreatmentDb(),
                DomainBuilderDatabase.getPatientPriorTreatmentDb());

        when(repository.findAllActive()).thenReturn(records);
        when(mapper.toModelList(records)).thenReturn(List.of(
                DomainBuilderDatabase.getPatientPriorTreatment(),
                DomainBuilderDatabase.getPatientPriorTreatment()));

        assertEquals(2, service.findAll().size());
    }

    @Test
    void findByActive_shouldReturnFilteredEntities() {
        List<PatientPriorTreatmentDb> records = List.of(DomainBuilderDatabase.getPatientPriorTreatmentDb());

        when(repository.findByActive(ActiveEnum.ACTIVE)).thenReturn(records);
        when(mapper.toModelList(records)).thenReturn(List.of(DomainBuilderDatabase.getPatientPriorTreatment()));

        assertEquals(1, service.findByActive(ActiveEnum.ACTIVE).size());
    }

    @Test
    void findByAppUserId_shouldReturnPatientRows() {
        List<PatientPriorTreatmentDb> records =
                List.of(DomainBuilderDatabase.getPatientPriorTreatmentDb(4242L, "CURRENT"));

        when(repository.findByAppUserId(4242L)).thenReturn(records);
        when(mapper.toModelList(records))
                .thenReturn(List.of(DomainBuilderDatabase.getPatientPriorTreatment(records.get(0))));

        List<PatientPriorTreatment> found = service.findByAppUserId(4242L);

        assertEquals(1, found.size());
        // CURRENT, not a bare "has taken it" - the distinction this table exists for.
        assertEquals("CURRENT", found.get(0).getCdk46Status());
    }
}
