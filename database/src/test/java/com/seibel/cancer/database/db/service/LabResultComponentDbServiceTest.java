package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.LabResultComponent;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.entity.LabResultComponentDb;
import com.seibel.cancer.database.db.mapper.LabResultComponentMapper;
import com.seibel.cancer.database.db.repository.LabResultComponentRepository;
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
class LabResultComponentDbServiceTest {

    @Mock
    private LabResultComponentRepository repository;

    @Mock
    private LabResultComponentMapper mapper;

    @InjectMocks
    private LabResultComponentDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        LabResultComponent item = DomainBuilderDatabase.getLabResultComponent();
        LabResultComponentDb mapped = new LabResultComponentDb();

        when(mapper.toDb(item)).thenReturn(mapped);
        when(repository.save(any(LabResultComponentDb.class))).thenReturn(mapped);
        when(mapper.toModel(mapped)).thenReturn(item);

        LabResultComponent created = service.create(item);

        ArgumentCaptor<LabResultComponentDb> captor = ArgumentCaptor.forClass(LabResultComponentDb.class);
        Mockito.verify(repository).save(captor.capture());
        LabResultComponentDb saved = captor.getValue();

        assertNotNull(created);
        assertNotNull(saved.getExtid());
        assertEquals(ActiveEnum.ACTIVE, saved.getActive());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        LabResultComponent item = DomainBuilderDatabase.getLabResultComponent();

        when(mapper.toDb(item)).thenReturn(new LabResultComponentDb());
        when(repository.save(any(LabResultComponentDb.class))).thenThrow(new RuntimeException("db down"));

        assertThrows(ServiceException.class, () -> service.create(item));
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        LabResultComponentDb existing = DomainBuilderDatabase.getLabResultComponentDb();
        String extid = existing.getExtid();

        LabResultComponent changes = LabResultComponent.builder()
                .componentName("Hemoglobin")
                .valueUnit("g/dL")
                .build();

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existing));
        when(repository.save(any(LabResultComponentDb.class))).thenReturn(existing);
        when(mapper.toModel(existing)).thenReturn(DomainBuilderDatabase.getLabResultComponent(existing));

        service.update(extid, changes);

        ArgumentCaptor<LabResultComponentDb> captor = ArgumentCaptor.forClass(LabResultComponentDb.class);
        Mockito.verify(repository).save(captor.capture());
        LabResultComponentDb saved = captor.getValue();

        assertEquals("Hemoglobin", saved.getComponentName());
        assertEquals("g/dL", saved.getValueUnit());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void update_shouldOnlyOverwriteNonNullFields() {
        LabResultComponentDb existing = DomainBuilderDatabase.getLabResultComponentDb();
        Long originalLabResultId = existing.getLabResultId();
        String extid = existing.getExtid();

        LabResultComponent changes = LabResultComponent.builder().componentName("Hematocrit").build();

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existing));
        when(repository.save(any(LabResultComponentDb.class))).thenReturn(existing);
        when(mapper.toModel(existing)).thenReturn(DomainBuilderDatabase.getLabResultComponent(existing));

        service.update(extid, changes);

        ArgumentCaptor<LabResultComponentDb> captor = ArgumentCaptor.forClass(LabResultComponentDb.class);
        Mockito.verify(repository).save(captor.capture());

        assertEquals("Hematocrit", captor.getValue().getComponentName());
        // The parent link must survive a partial update.
        assertEquals(originalLabResultId, captor.getValue().getLabResultId());
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        String extid = UUID.randomUUID().toString();
        LabResultComponent changes = LabResultComponent.builder().componentName("Hemoglobin").build();

        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        assertThrows(ServiceException.class, () -> service.update(extid, changes));
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        LabResultComponentDb existing = DomainBuilderDatabase.getLabResultComponentDb();
        String extid = existing.getExtid();
        LabResultComponent changes = LabResultComponent.builder().componentName("Hemoglobin").build();

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existing));
        when(repository.save(any(LabResultComponentDb.class))).thenThrow(new RuntimeException("db down"));

        assertThrows(ServiceException.class, () -> service.update(extid, changes));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        LabResultComponentDb existing = DomainBuilderDatabase.getLabResultComponentDb();
        String extid = existing.getExtid();

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existing));
        when(repository.save(any(LabResultComponentDb.class))).thenReturn(existing);

        boolean deleted = service.delete(extid);

        ArgumentCaptor<LabResultComponentDb> captor = ArgumentCaptor.forClass(LabResultComponentDb.class);
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
        LabResultComponentDb existing = DomainBuilderDatabase.getLabResultComponentDb();
        LabResultComponent expected = DomainBuilderDatabase.getLabResultComponent(existing);

        when(repository.findByExtid(existing.getExtid())).thenReturn(Optional.of(existing));
        when(mapper.toModel(existing)).thenReturn(expected);

        LabResultComponent found = service.findByExtid(existing.getExtid());

        assertNotNull(found);
        assertEquals(expected.getComponentName(), found.getComponentName());
    }

    @Test
    void findByExtid_shouldThrowException_whenNotFound() {
        String extid = UUID.randomUUID().toString();

        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        assertThrows(ServiceException.class, () -> service.findByExtid(extid));
    }

    @Test
    void findByLabResultId_shouldReturnComponentsOfOnePanel() {
        List<LabResultComponentDb> records = List.of(
                DomainBuilderDatabase.getLabResultComponentDb(4242L, "Hemoglobin"),
                DomainBuilderDatabase.getLabResultComponentDb(4242L, "Hematocrit"));

        when(repository.findByLabResultId(4242L)).thenReturn(records);
        when(mapper.toModelList(records)).thenReturn(List.of(
                DomainBuilderDatabase.getLabResultComponent(),
                DomainBuilderDatabase.getLabResultComponent()));

        assertEquals(2, service.findByLabResultId(4242L).size());
    }

    @Test
    void findAll_shouldReturnAllEntities() {
        List<LabResultComponentDb> records = List.of(
                DomainBuilderDatabase.getLabResultComponentDb(),
                DomainBuilderDatabase.getLabResultComponentDb());

        when(repository.findAllActive()).thenReturn(records);
        when(mapper.toModelList(records)).thenReturn(List.of(
                DomainBuilderDatabase.getLabResultComponent(),
                DomainBuilderDatabase.getLabResultComponent()));

        assertEquals(2, service.findAll().size());
    }

    @Test
    void findByActive_shouldReturnFilteredEntities() {
        List<LabResultComponentDb> records = List.of(DomainBuilderDatabase.getLabResultComponentDb());

        when(repository.findByActive(ActiveEnum.ACTIVE)).thenReturn(records);
        when(mapper.toModelList(records)).thenReturn(List.of(DomainBuilderDatabase.getLabResultComponent()));

        assertEquals(1, service.findByActive(ActiveEnum.ACTIVE).size());
    }
}
