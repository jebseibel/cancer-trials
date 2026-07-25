package com.seibel.cancer.database.db.service;

import com.seibel.cancer.common.domain.Location;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.LocationDb;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.mapper.LocationMapper;
import com.seibel.cancer.database.db.repository.LocationRepository;
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
class LocationDbServiceTest {

    @Mock
    private LocationRepository repository;

    @Mock
    private LocationMapper mapper;

    @InjectMocks
    private LocationDbService service;

    @Test
    void create_shouldGenerateUuidAndSetFields() {
        // Arrange
        Location input = DomainBuilderDatabase.getLocation();
        LocationDb savedDb = DomainBuilderDatabase.getLocationDb(input.getTrialId(), input.getFacility());
        Location expectedDomain = DomainBuilderDatabase.getLocation(savedDb);

        when(mapper.toDb(input)).thenReturn(new LocationDb());
        when(repository.save(any(LocationDb.class))).thenReturn(savedDb);
        when(mapper.toModel(savedDb)).thenReturn(expectedDomain);

        // Act
        Location result = service.create(input);

        // Assert
        assertNotNull(result);
        ArgumentCaptor<LocationDb> captor = ArgumentCaptor.forClass(LocationDb.class);
        verify(repository).save(captor.capture());

        LocationDb captured = captor.getValue();
        assertNotNull(captured.getExtid());
        assertEquals(ActiveEnum.ACTIVE, captured.getActive());
        assertNotNull(captured.getCreatedAt());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(savedDb);
    }

    @Test
    void create_shouldThrowException_whenRepositoryFails() {
        // Arrange
        Location input = DomainBuilderDatabase.getLocation();
        when(mapper.toDb(input)).thenReturn(new LocationDb());
        when(repository.save(any(LocationDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.create(input));
        verify(repository).save(any(LocationDb.class));
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldUpdateFieldsAndTimestamp() {
        // Arrange
        String extid = "existing-extid";
        LocationDb existingDb = DomainBuilderDatabase.getLocationDb(1L, "Old Facility", null, extid);

        Location changes = Location.builder()
                .facility("Updated Facility")
                .status("RECRUITING")
                .build();

        LocationDb updatedDb = DomainBuilderDatabase.getLocationDb(1L, "Updated Facility", "RECRUITING", extid);
        Location expectedDomain = DomainBuilderDatabase.getLocation(updatedDb);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(LocationDb.class))).thenReturn(updatedDb);
        when(mapper.toModel(updatedDb)).thenReturn(expectedDomain);

        // Act
        Location result = service.update(extid, changes);

        // Assert
        assertNotNull(result);
        verify(repository).findByExtid(extid);

        ArgumentCaptor<LocationDb> captor = ArgumentCaptor.forClass(LocationDb.class);
        verify(repository).save(captor.capture());

        LocationDb captured = captor.getValue();
        assertEquals("Updated Facility", captured.getFacility());
        assertEquals("RECRUITING", captured.getStatus());
        assertNotNull(captured.getUpdatedAt());
        verify(mapper).toModel(updatedDb);
    }

    @Test
    void update_shouldThrowException_whenNotFound() {
        // Arrange
        String extid = "nonexistent-extid";
        Location changes = Location.builder().facility("Doesn't matter").build();
        when(repository.findByExtid(extid)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.update(extid, changes));
        verify(repository).findByExtid(extid);
        verify(repository, never()).save(any());
        verify(mapper, never()).toModel(any());
    }

    @Test
    void update_shouldThrowException_whenRepositoryFails() {
        // Arrange
        String extid = "existing-extid";
        LocationDb existingDb = DomainBuilderDatabase.getLocationDb(1L, "Facility", null, extid);
        Location changes = Location.builder().facility("New Facility").build();

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(LocationDb.class))).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        assertThrows(ServiceException.class, () -> service.update(extid, changes));
    }

    @Test
    void delete_shouldSetDeletedAtAndInactive() {
        // Arrange
        String extid = "existing-extid";
        LocationDb existingDb = DomainBuilderDatabase.getLocationDb(1L, "Facility", null, extid);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(existingDb));
        when(repository.save(any(LocationDb.class))).thenReturn(existingDb);

        // Act
        boolean result = service.delete(extid);

        // Assert
        assertTrue(result);

        ArgumentCaptor<LocationDb> captor = ArgumentCaptor.forClass(LocationDb.class);
        verify(repository).save(captor.capture());

        LocationDb captured = captor.getValue();
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
    void findByExtid_shouldReturnLocation_whenExists() {
        // Arrange
        String extid = "existing-extid";
        LocationDb db = DomainBuilderDatabase.getLocationDb(1L, "Facility", null, extid);
        Location expectedDomain = DomainBuilderDatabase.getLocation(db);

        when(repository.findByExtid(extid)).thenReturn(Optional.of(db));
        when(mapper.toModel(db)).thenReturn(expectedDomain);

        // Act
        Location result = service.findByExtid(extid);

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
    void findAll_shouldReturnAllLocations() {
        // Arrange
        LocationDb db1 = DomainBuilderDatabase.getLocationDb();
        LocationDb db2 = DomainBuilderDatabase.getLocationDb();
        List<LocationDb> dbList = Arrays.asList(db1, db2);

        Location domain1 = DomainBuilderDatabase.getLocation(db1);
        Location domain2 = DomainBuilderDatabase.getLocation(db2);
        List<Location> domainList = Arrays.asList(domain1, domain2);

        when(repository.findAllActive()).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Location> result = service.findAll();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findAllActive();
        verify(mapper).toModelList(dbList);
    }

    @Test
    void findByActive_shouldReturnFilteredLocations() {
        // Arrange
        LocationDb db1 = DomainBuilderDatabase.getLocationDb();
        db1.setActive(ActiveEnum.ACTIVE);
        LocationDb db2 = DomainBuilderDatabase.getLocationDb();
        db2.setActive(ActiveEnum.ACTIVE);
        List<LocationDb> dbList = Arrays.asList(db1, db2);

        Location domain1 = DomainBuilderDatabase.getLocation(db1);
        Location domain2 = DomainBuilderDatabase.getLocation(db2);
        List<Location> domainList = Arrays.asList(domain1, domain2);

        when(repository.findByActive(any(ActiveEnum.class))).thenReturn(dbList);
        when(mapper.toModelList(dbList)).thenReturn(domainList);

        // Act
        List<Location> result = service.findByActive(ActiveEnum.ACTIVE);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(repository).findByActive(any(ActiveEnum.class));
        verify(mapper).toModelList(dbList);
    }
}
