package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.PatientDiagnosis;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.entity.PatientDiagnosisDb;
import com.seibel.cancer.database.db.mapper.PatientDiagnosisMapper;
import com.seibel.cancer.database.db.repository.PatientDiagnosisRepository;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientDiagnosisDbServiceTest {

    @Mock
    private PatientDiagnosisRepository repository;

    @Mock
    private PatientDiagnosisMapper mapper;

    @InjectMocks
    private PatientDiagnosisDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        PatientDiagnosis item = DomainBuilderDatabase.getPatientDiagnosis();
        PatientDiagnosisDb mapped = new PatientDiagnosisDb();

        when(mapper.toDb(item)).thenReturn(mapped);
        when(repository.save(any(PatientDiagnosisDb.class))).thenReturn(mapped);
        when(mapper.toModel(mapped)).thenReturn(item);

        PatientDiagnosis created = service.create(item);

        ArgumentCaptor<PatientDiagnosisDb> captor = ArgumentCaptor.forClass(PatientDiagnosisDb.class);
        Mockito.verify(repository).save(captor.capture());
        PatientDiagnosisDb saved = captor.getValue();

        assertNotNull(created);
        assertNotNull(saved.getExtid());
        assertEquals(ActiveEnum.ACTIVE, saved.getActive());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        PatientDiagnosis item = DomainBuilderDatabase.getPatientDiagnosis();

        when(mapper.toDb(item)).thenReturn(new PatientDiagnosisDb());
        when(repository.save(any(PatientDiagnosisDb.class))).thenThrow(new RuntimeException("db down"));

        assertThrows(ServiceException.class, () -> service.create(item));
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        PatientDiagnosisDb existing = DomainBuilderDatabase.getPatientDiagnosisDb();
        String extid = existing.getExtid();

        PatientDiagnosis changes = PatientDiagnosis.builder()
                .cancerType("Breast Cancer")
                .stage("IV")
                .ecogStatus(1)
                .build();

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existing));
        when(repository.save(any(PatientDiagnosisDb.class))).thenReturn(existing);
        when(mapper.toModel(existing)).thenReturn(DomainBuilderDatabase.getPatientDiagnosis(existing));

        service.update(extid, changes);

        ArgumentCaptor<PatientDiagnosisDb> captor = ArgumentCaptor.forClass(PatientDiagnosisDb.class);
        Mockito.verify(repository).save(captor.capture());
        PatientDiagnosisDb saved = captor.getValue();

        assertEquals("Breast Cancer", saved.getCancerType());
        assertEquals("IV", saved.getStage());
        assertEquals(Integer.valueOf(1), saved.getEcogStatus());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void update_shouldOnlyOverwriteNonNullFields() {
        PatientDiagnosisDb existing = DomainBuilderDatabase.getPatientDiagnosisDb();
        String originalStage = existing.getStage();
        Long originalAppUserId = existing.getAppUserId();
        String extid = existing.getExtid();

        PatientDiagnosis changes = PatientDiagnosis.builder().cancerType("Lung Cancer").build();

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existing));
        when(repository.save(any(PatientDiagnosisDb.class))).thenReturn(existing);
        when(mapper.toModel(existing)).thenReturn(DomainBuilderDatabase.getPatientDiagnosis(existing));

        service.update(extid, changes);

        ArgumentCaptor<PatientDiagnosisDb> captor = ArgumentCaptor.forClass(PatientDiagnosisDb.class);
        Mockito.verify(repository).save(captor.capture());

        assertEquals("Lung Cancer", captor.getValue().getCancerType());
        assertEquals(originalStage, captor.getValue().getStage());
        assertEquals(originalAppUserId, captor.getValue().getAppUserId());
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        String extid = UUID.randomUUID().toString();
        PatientDiagnosis changes = PatientDiagnosis.builder().cancerType("Lung Cancer").build();

        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        assertThrows(ServiceException.class, () -> service.update(extid, changes));
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        PatientDiagnosisDb existing = DomainBuilderDatabase.getPatientDiagnosisDb();
        String extid = existing.getExtid();
        PatientDiagnosis changes = PatientDiagnosis.builder().cancerType("Lung Cancer").build();

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existing));
        when(repository.save(any(PatientDiagnosisDb.class))).thenThrow(new RuntimeException("db down"));

        assertThrows(ServiceException.class, () -> service.update(extid, changes));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        PatientDiagnosisDb existing = DomainBuilderDatabase.getPatientDiagnosisDb();
        String extid = existing.getExtid();

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existing));
        when(repository.save(any(PatientDiagnosisDb.class))).thenReturn(existing);

        boolean deleted = service.delete(extid);

        ArgumentCaptor<PatientDiagnosisDb> captor = ArgumentCaptor.forClass(PatientDiagnosisDb.class);
        Mockito.verify(repository).save(captor.capture());

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
        PatientDiagnosisDb existing = DomainBuilderDatabase.getPatientDiagnosisDb();
        PatientDiagnosis expected = DomainBuilderDatabase.getPatientDiagnosis(existing);

        when(repository.findByExtid(existing.getExtid())).thenReturn(Optional.of(existing));
        when(mapper.toModel(existing)).thenReturn(expected);

        PatientDiagnosis found = service.findByExtid(existing.getExtid());

        assertNotNull(found);
        assertEquals(expected.getCancerType(), found.getCancerType());
    }

    @Test
    void findByExtid_shouldThrowException_whenNotFound() {
        String extid = UUID.randomUUID().toString();

        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        assertThrows(ServiceException.class, () -> service.findByExtid(extid));
    }

    @Test
    void findByAppUserId_shouldReturnEntities() {
        List<PatientDiagnosisDb> records = List.of(
                DomainBuilderDatabase.getPatientDiagnosisDb(4242L, "Breast Cancer"));

        when(repository.findByAppUserId(4242L)).thenReturn(records);
        when(mapper.toModelList(records)).thenReturn(List.of(
                DomainBuilderDatabase.getPatientDiagnosis(records.get(0))));

        List<PatientDiagnosis> found = service.findByAppUserId(4242L);

        assertEquals(1, found.size());
        assertEquals("Breast Cancer", found.get(0).getCancerType());
    }

    @Test
    void findByAppUserId_shouldReturnEmptyList_whenNoneFound() {
        when(repository.findByAppUserId(123456L)).thenReturn(List.of());
        when(mapper.toModelList(List.of())).thenReturn(List.of());

        assertTrue(service.findByAppUserId(123456L).isEmpty());
    }

    @Test
    void findAll_shouldReturnAllEntities() {
        List<PatientDiagnosisDb> records = List.of(
                DomainBuilderDatabase.getPatientDiagnosisDb(),
                DomainBuilderDatabase.getPatientDiagnosisDb());

        when(repository.findAllActive()).thenReturn(records);
        when(mapper.toModelList(records)).thenReturn(List.of(
                DomainBuilderDatabase.getPatientDiagnosis(),
                DomainBuilderDatabase.getPatientDiagnosis()));

        assertEquals(2, service.findAll().size());
    }

    @Test
    void findByActive_shouldReturnFilteredEntities() {
        List<PatientDiagnosisDb> records = List.of(DomainBuilderDatabase.getPatientDiagnosisDb());

        when(repository.findByActive(ActiveEnum.ACTIVE)).thenReturn(records);
        when(mapper.toModelList(records)).thenReturn(List.of(DomainBuilderDatabase.getPatientDiagnosis()));

        assertEquals(1, service.findByActive(ActiveEnum.ACTIVE).size());
    }
}
