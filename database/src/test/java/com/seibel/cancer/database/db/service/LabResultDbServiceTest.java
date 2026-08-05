package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.LabResult;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.entity.LabResultDb;
import com.seibel.cancer.database.db.mapper.LabResultMapper;
import com.seibel.cancer.database.db.repository.LabResultRepository;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class LabResultDbServiceTest {

    @Mock
    private LabResultRepository repository;

    @Mock
    private LabResultMapper mapper;

    @InjectMocks
    private LabResultDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        LabResult item = DomainBuilderDatabase.getLabResult();
        LabResultDb mapped = new LabResultDb();

        when(mapper.toDb(item)).thenReturn(mapped);
        when(repository.save(any(LabResultDb.class))).thenReturn(mapped);
        when(mapper.toModel(mapped)).thenReturn(item);

        LabResult created = service.create(item);

        ArgumentCaptor<LabResultDb> captor = ArgumentCaptor.forClass(LabResultDb.class);
        Mockito.verify(repository).save(captor.capture());
        LabResultDb saved = captor.getValue();

        assertNotNull(created);
        assertNotNull(saved.getExtid());
        assertEquals(ActiveEnum.ACTIVE, saved.getActive());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        LabResult item = DomainBuilderDatabase.getLabResult();

        when(mapper.toDb(item)).thenReturn(new LabResultDb());
        when(repository.save(any(LabResultDb.class))).thenThrow(new RuntimeException("db down"));

        assertThrows(ServiceException.class, () -> service.create(item));
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        LabResultDb existing = DomainBuilderDatabase.getLabResultDb();
        String extid = existing.getExtid();

        LabResult changes = LabResult.builder()
                .testName("Hemoglobin A1C")
                .valueQuantity(new BigDecimal("5.100000"))
                .build();

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existing));
        when(repository.save(any(LabResultDb.class))).thenReturn(existing);
        when(mapper.toModel(existing)).thenReturn(DomainBuilderDatabase.getLabResult(existing));

        service.update(extid, changes);

        ArgumentCaptor<LabResultDb> captor = ArgumentCaptor.forClass(LabResultDb.class);
        Mockito.verify(repository).save(captor.capture());
        LabResultDb saved = captor.getValue();

        assertEquals("Hemoglobin A1C", saved.getTestName());
        assertEquals(new BigDecimal("5.100000"), saved.getValueQuantity());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void update_shouldOnlyOverwriteNonNullFields() {
        LabResultDb existing = DomainBuilderDatabase.getLabResultDb();
        String originalUnit = existing.getValueUnit();
        String extid = existing.getExtid();

        LabResult changes = LabResult.builder().testName("Glucose").build();

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existing));
        when(repository.save(any(LabResultDb.class))).thenReturn(existing);
        when(mapper.toModel(existing)).thenReturn(DomainBuilderDatabase.getLabResult(existing));

        service.update(extid, changes);

        ArgumentCaptor<LabResultDb> captor = ArgumentCaptor.forClass(LabResultDb.class);
        Mockito.verify(repository).save(captor.capture());

        assertEquals("Glucose", captor.getValue().getTestName());
        assertEquals(originalUnit, captor.getValue().getValueUnit());
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        String extid = UUID.randomUUID().toString();
        LabResult changes = LabResult.builder().testName("Glucose").build();

        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        assertThrows(ServiceException.class, () -> service.update(extid, changes));
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        LabResultDb existing = DomainBuilderDatabase.getLabResultDb();
        String extid = existing.getExtid();
        LabResult changes = LabResult.builder().testName("Glucose").build();

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existing));
        when(repository.save(any(LabResultDb.class))).thenThrow(new RuntimeException("db down"));

        assertThrows(ServiceException.class, () -> service.update(extid, changes));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        LabResultDb existing = DomainBuilderDatabase.getLabResultDb();
        String extid = existing.getExtid();

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existing));
        when(repository.save(any(LabResultDb.class))).thenReturn(existing);

        boolean deleted = service.delete(extid);

        ArgumentCaptor<LabResultDb> captor = ArgumentCaptor.forClass(LabResultDb.class);
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
        LabResultDb existing = DomainBuilderDatabase.getLabResultDb();
        LabResult expected = DomainBuilderDatabase.getLabResult(existing);

        when(repository.findByExtid(existing.getExtid())).thenReturn(Optional.of(existing));
        when(mapper.toModel(existing)).thenReturn(expected);

        LabResult found = service.findByExtid(existing.getExtid());

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
    void findByFhirResourceId_shouldReturnNull_whenNotFound() {
        when(repository.findByFhirResourceId("Obs-missing")).thenReturn(Optional.empty());

        // Returns null rather than throwing - ingestion uses this to decide insert-vs-update.
        assertNull(service.findByFhirResourceId("Obs-missing"));
    }

    @Test
    void findAll_shouldReturnAllEntities() {
        List<LabResultDb> records = List.of(
                DomainBuilderDatabase.getLabResultDb(),
                DomainBuilderDatabase.getLabResultDb());

        when(repository.findAllActive()).thenReturn(records);
        when(mapper.toModelList(records)).thenReturn(List.of(
                DomainBuilderDatabase.getLabResult(),
                DomainBuilderDatabase.getLabResult()));

        assertEquals(2, service.findAll().size());
    }

    @Test
    void findByActive_shouldReturnFilteredEntities() {
        List<LabResultDb> records = List.of(DomainBuilderDatabase.getLabResultDb());

        when(repository.findByActive(ActiveEnum.ACTIVE)).thenReturn(records);
        when(mapper.toModelList(records)).thenReturn(List.of(DomainBuilderDatabase.getLabResult()));

        assertEquals(1, service.findByActive(ActiveEnum.ACTIVE).size());
    }
}
