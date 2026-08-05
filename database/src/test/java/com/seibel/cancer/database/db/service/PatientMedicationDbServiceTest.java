package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.PatientMedication;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.entity.PatientMedicationDb;
import com.seibel.cancer.database.db.mapper.PatientMedicationMapper;
import com.seibel.cancer.database.db.repository.PatientMedicationRepository;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientMedicationDbServiceTest {

    @Mock
    private PatientMedicationRepository repository;

    @Mock
    private PatientMedicationMapper mapper;

    @InjectMocks
    private PatientMedicationDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        PatientMedication item = DomainBuilderDatabase.getPatientMedication();
        PatientMedicationDb mapped = new PatientMedicationDb();

        when(mapper.toDb(item)).thenReturn(mapped);
        when(repository.save(any(PatientMedicationDb.class))).thenReturn(mapped);
        when(mapper.toModel(mapped)).thenReturn(item);

        PatientMedication created = service.create(item);

        ArgumentCaptor<PatientMedicationDb> captor = ArgumentCaptor.forClass(PatientMedicationDb.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        PatientMedicationDb saved = captor.getValue();

        assertNotNull(created);
        assertNotNull(saved.getExtid());
        assertEquals(ActiveEnum.ACTIVE, saved.getActive());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        PatientMedication item = DomainBuilderDatabase.getPatientMedication();

        when(mapper.toDb(item)).thenReturn(new PatientMedicationDb());
        when(repository.save(any(PatientMedicationDb.class))).thenThrow(new RuntimeException("db down"));

        assertThrows(ServiceException.class, () -> service.create(item));
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        PatientMedicationDb existing = DomainBuilderDatabase.getPatientMedicationDb();
        String extid = existing.getExtid();

        PatientMedication changes = PatientMedication.builder()
                .medicationName("Tamoxifen")
                .status("stopped")
                .build();

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existing));
        when(repository.save(any(PatientMedicationDb.class))).thenReturn(existing);
        when(mapper.toModel(existing)).thenReturn(DomainBuilderDatabase.getPatientMedication(existing));

        service.update(extid, changes);

        ArgumentCaptor<PatientMedicationDb> captor = ArgumentCaptor.forClass(PatientMedicationDb.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());
        PatientMedicationDb saved = captor.getValue();

        assertEquals("Tamoxifen", saved.getMedicationName());
        assertEquals("stopped", saved.getStatus());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void update_shouldOnlyOverwriteNonNullFields() {
        PatientMedicationDb existing = DomainBuilderDatabase.getPatientMedicationDb();
        String originalRoute = existing.getRoute();
        String extid = existing.getExtid();

        // Only medicationName is set - every other field must survive untouched.
        PatientMedication changes = PatientMedication.builder().medicationName("Anastrozole").build();

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existing));
        when(repository.save(any(PatientMedicationDb.class))).thenReturn(existing);
        when(mapper.toModel(existing)).thenReturn(DomainBuilderDatabase.getPatientMedication(existing));

        service.update(extid, changes);

        ArgumentCaptor<PatientMedicationDb> captor = ArgumentCaptor.forClass(PatientMedicationDb.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());

        assertEquals("Anastrozole", captor.getValue().getMedicationName());
        assertEquals(originalRoute, captor.getValue().getRoute());
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        String extid = UUID.randomUUID().toString();
        PatientMedication changes = PatientMedication.builder().medicationName("Tamoxifen").build();

        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        assertThrows(ServiceException.class, () -> service.update(extid, changes));
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        PatientMedicationDb existing = DomainBuilderDatabase.getPatientMedicationDb();
        String extid = existing.getExtid();
        PatientMedication changes = PatientMedication.builder().medicationName("Tamoxifen").build();

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existing));
        when(repository.save(any(PatientMedicationDb.class))).thenThrow(new RuntimeException("db down"));

        assertThrows(ServiceException.class, () -> service.update(extid, changes));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        PatientMedicationDb existing = DomainBuilderDatabase.getPatientMedicationDb();
        String extid = existing.getExtid();

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existing));
        when(repository.save(any(PatientMedicationDb.class))).thenReturn(existing);

        boolean deleted = service.delete(extid);

        ArgumentCaptor<PatientMedicationDb> captor = ArgumentCaptor.forClass(PatientMedicationDb.class);
        org.mockito.Mockito.verify(repository).save(captor.capture());

        assertTrue(deleted);
        assertNotNull(captor.getValue().getDeletedAt());
        assertEquals(ActiveEnum.INACTIVE, captor.getValue().getActive());
    }

    @Test
    void delete_shouldThrowException_whenNotFound() {
        String extid = UUID.randomUUID().toString();

        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        assertThrows(ServiceException.class, () -> service.delete(extid));
    }

    @Test
    void findByExtid_shouldReturnEntity_whenExists() {
        PatientMedicationDb existing = DomainBuilderDatabase.getPatientMedicationDb();
        PatientMedication expected = DomainBuilderDatabase.getPatientMedication(existing);

        when(repository.findByExtid(existing.getExtid())).thenReturn(Optional.of(existing));
        when(mapper.toModel(existing)).thenReturn(expected);

        PatientMedication found = service.findByExtid(existing.getExtid());

        assertNotNull(found);
        assertEquals(expected.getFhirResourceId(), found.getFhirResourceId());
    }

    @Test
    void findByExtid_shouldThrowException_whenNotFound() {
        String extid = UUID.randomUUID().toString();

        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        assertThrows(ServiceException.class, () -> service.findByExtid(extid));
    }

    @Test
    void findByFhirResourceId_shouldReturnEntity_whenExists() {
        PatientMedicationDb existing = DomainBuilderDatabase.getPatientMedicationDb("MedReq-999", "Tamoxifen");
        PatientMedication expected = DomainBuilderDatabase.getPatientMedication(existing);

        when(repository.findByFhirResourceId("MedReq-999")).thenReturn(Optional.of(existing));
        when(mapper.toModel(existing)).thenReturn(expected);

        assertNotNull(service.findByFhirResourceId("MedReq-999"));
    }

    @Test
    void findByFhirResourceId_shouldReturnNull_whenNotFound() {
        when(repository.findByFhirResourceId("MedReq-missing")).thenReturn(Optional.empty());

        // Returns null rather than throwing - ingestion uses this to decide insert-vs-update.
        assertNull(service.findByFhirResourceId("MedReq-missing"));
    }

    @Test
    void findAll_shouldReturnAllEntities() {
        List<PatientMedicationDb> records = List.of(
                DomainBuilderDatabase.getPatientMedicationDb(),
                DomainBuilderDatabase.getPatientMedicationDb());

        when(repository.findAllActive()).thenReturn(records);
        when(mapper.toModelList(records)).thenReturn(List.of(
                DomainBuilderDatabase.getPatientMedication(),
                DomainBuilderDatabase.getPatientMedication()));

        assertEquals(2, service.findAll().size());
    }

    @Test
    void findByActive_shouldReturnFilteredEntities() {
        List<PatientMedicationDb> records = List.of(DomainBuilderDatabase.getPatientMedicationDb());

        when(repository.findByActive(ActiveEnum.ACTIVE)).thenReturn(records);
        when(mapper.toModelList(records)).thenReturn(List.of(DomainBuilderDatabase.getPatientMedication()));

        assertEquals(1, service.findByActive(ActiveEnum.ACTIVE).size());
    }
}
