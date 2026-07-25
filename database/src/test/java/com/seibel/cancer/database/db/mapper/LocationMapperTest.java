package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.Location;
import com.seibel.cancer.database.db.entity.LocationDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LocationMapperTest {

    private LocationMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new LocationMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        // Arrange
        LocationDb db = DomainBuilderDatabase.getLocationDb();

        // Act
        Location domain = mapper.toModel(db);

        // Assert
        assertNotNull(domain);
        assertEquals(db.getExtid(), domain.getExtid());
        assertEquals(db.getTrialId(), domain.getTrialId());
        assertEquals(db.getFacility(), domain.getFacility());
        assertEquals(db.getCity(), domain.getCity());
        assertEquals(db.getState(), domain.getState());
        assertEquals(db.getZip(), domain.getZip());
        assertEquals(db.getCountry(), domain.getCountry());
        assertEquals(db.getStatus(), domain.getStatus());
        assertEquals(db.getLatitude(), domain.getLatitude());
        assertEquals(db.getLongitude(), domain.getLongitude());
        assertEquals(db.getCreatedAt(), domain.getCreatedAt());
        assertEquals(db.getUpdatedAt(), domain.getUpdatedAt());
        assertEquals(db.getDeletedAt(), domain.getDeletedAt());
        assertEquals(db.getActive(), domain.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        // Arrange
        Location domain = DomainBuilderDatabase.getLocation();

        // Act
        LocationDb db = mapper.toDb(domain);

        // Assert
        assertNotNull(db);
        assertEquals(domain.getExtid(), db.getExtid());
        assertEquals(domain.getTrialId(), db.getTrialId());
        assertEquals(domain.getFacility(), db.getFacility());
        assertEquals(domain.getCity(), db.getCity());
        assertEquals(domain.getState(), db.getState());
        assertEquals(domain.getZip(), db.getZip());
        assertEquals(domain.getCountry(), db.getCountry());
        assertEquals(domain.getStatus(), db.getStatus());
        assertEquals(domain.getLatitude(), db.getLatitude());
        assertEquals(domain.getLongitude(), db.getLongitude());
        assertEquals(domain.getCreatedAt(), db.getCreatedAt());
        assertEquals(domain.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(domain.getDeletedAt(), db.getDeletedAt());
        assertEquals(domain.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        // Arrange
        LocationDb db1 = DomainBuilderDatabase.getLocationDb();
        LocationDb db2 = DomainBuilderDatabase.getLocationDb();
        List<LocationDb> dbList = Arrays.asList(db1, db2);

        // Act
        List<Location> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals(db1.getExtid(), domainList.get(0).getExtid());
        assertEquals(db2.getExtid(), domainList.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        // Arrange
        List<LocationDb> dbList = Arrays.asList();

        // Act
        List<Location> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        // Act
        List<Location> domainList = mapper.toModelList(null);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toDbList_shouldMapAllItems() {
        // Arrange
        Location domain1 = DomainBuilderDatabase.getLocation();
        Location domain2 = DomainBuilderDatabase.getLocation();
        List<Location> domainList = Arrays.asList(domain1, domain2);

        // Act
        List<LocationDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(2, dbList.size());
        assertEquals(domain1.getExtid(), dbList.get(0).getExtid());
        assertEquals(domain2.getExtid(), dbList.get(1).getExtid());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        // Arrange
        List<Location> domainList = Arrays.asList();

        // Act
        List<LocationDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        // Act
        List<LocationDb> dbList = mapper.toDbList(null);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }
}
