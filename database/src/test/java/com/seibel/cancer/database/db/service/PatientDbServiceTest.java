package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.Patient;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.entity.PatientDb;
import com.seibel.cancer.database.db.mapper.PatientMapper;
import com.seibel.cancer.database.db.repository.PatientRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientDbServiceTest {

    @Mock
    private PatientRepository repository;

    @Mock
    private PatientMapper mapper;

    @InjectMocks
    private PatientDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        Patient input = DomainBuilderDatabase.getPatient();
        PatientDb mapped = DomainBuilderDatabase.getPatientDb();

        when(mapper.toDb(input)).thenReturn(mapped);
        when(repository.save(any(PatientDb.class))).thenReturn(mapped);
        when(mapper.toModel(mapped)).thenReturn(input);

        Patient result = service.create(input);

        ArgumentCaptor<PatientDb> captor = ArgumentCaptor.forClass(PatientDb.class);
        verify(repository).save(captor.capture());
        PatientDb saved = captor.getValue();

        assertNotNull(result);
        assertNotNull(saved.getExtid());
        assertEquals(ActiveEnum.ACTIVE, saved.getActive());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        Patient input = DomainBuilderDatabase.getPatient();

        when(mapper.toDb(input)).thenReturn(DomainBuilderDatabase.getPatientDb());
        when(repository.save(any(PatientDb.class))).thenThrow(new RuntimeException("boom"));

        assertThrows(ServiceException.class, () -> service.create(input));
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        String extid = UUID.randomUUID().toString();
        PatientDb existing = DomainBuilderDatabase.getPatientDb("Old Name", "F", extid);
        Patient update = Patient.builder().displayName("New Name").build();

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existing));
        when(repository.save(any(PatientDb.class))).thenReturn(existing);
        when(mapper.toModel(existing)).thenReturn(update);

        service.update(extid, update);

        ArgumentCaptor<PatientDb> captor = ArgumentCaptor.forClass(PatientDb.class);
        verify(repository).save(captor.capture());
        PatientDb saved = captor.getValue();

        assertEquals("New Name", saved.getDisplayName());
        // Null fields on the update must leave the stored value alone.
        assertEquals("F", saved.getSex());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        String extid = UUID.randomUUID().toString();
        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        assertThrows(ServiceException.class,
                () -> service.update(extid, DomainBuilderDatabase.getPatient()));
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        String extid = UUID.randomUUID().toString();
        when(repository.findByExtid(extid))
                .thenReturn(Optional.of(DomainBuilderDatabase.getPatientDb()));
        when(repository.save(any(PatientDb.class))).thenThrow(new RuntimeException("boom"));

        assertThrows(ServiceException.class,
                () -> service.update(extid, DomainBuilderDatabase.getPatient()));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        String extid = UUID.randomUUID().toString();
        PatientDb existing = DomainBuilderDatabase.getPatientDb(null, null, extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existing));
        when(repository.save(any(PatientDb.class))).thenReturn(existing);

        assertTrue(service.delete(extid));

        ArgumentCaptor<PatientDb> captor = ArgumentCaptor.forClass(PatientDb.class);
        verify(repository).save(captor.capture());
        PatientDb saved = captor.getValue();

        assertNotNull(saved.getDeletedAt());
        assertEquals(ActiveEnum.INACTIVE, saved.getActive());
    }

    @Test
    void delete_shouldThrowException_whenNotFound() {
        String extid = UUID.randomUUID().toString();
        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        assertThrows(ServiceException.class, () -> service.delete(extid));
    }

    @Test
    void findByExtid_shouldReturnEntity_whenExists() {
        String extid = UUID.randomUUID().toString();
        PatientDb existing = DomainBuilderDatabase.getPatientDb(null, null, extid);
        Patient domain = DomainBuilderDatabase.getPatient(existing);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existing));
        when(mapper.toModel(existing)).thenReturn(domain);

        Patient result = service.findByExtid(extid);

        assertNotNull(result);
        assertEquals(existing.getDisplayName(), result.getDisplayName());
    }

    @Test
    void findByExtid_shouldThrowException_whenNotFound() {
        String extid = UUID.randomUUID().toString();
        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        assertThrows(ServiceException.class, () -> service.findByExtid(extid));
    }

    @Test
    void findAll_shouldReturnAllEntities() {
        List<PatientDb> records = List.of(
                DomainBuilderDatabase.getPatientDb(),
                DomainBuilderDatabase.getPatientDb());

        when(repository.findAllActive()).thenReturn(records);
        when(mapper.toModelList(records)).thenReturn(List.of(
                DomainBuilderDatabase.getPatient(),
                DomainBuilderDatabase.getPatient()));

        assertEquals(2, service.findAll().size());
    }

    @Test
    void findByActive_shouldReturnFilteredEntities() {
        List<PatientDb> records = List.of(DomainBuilderDatabase.getPatientDb());

        when(repository.findByActive(ActiveEnum.ACTIVE)).thenReturn(records);
        when(mapper.toModelList(records)).thenReturn(List.of(DomainBuilderDatabase.getPatient()));

        assertEquals(1, service.findByActive(ActiveEnum.ACTIVE).size());
    }
}
