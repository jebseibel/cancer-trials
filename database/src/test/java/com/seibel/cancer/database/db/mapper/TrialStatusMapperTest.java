package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.TrialStatus;
import com.seibel.cancer.database.db.entity.TrialStatusDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TrialStatusMapperTest {

    private TrialStatusMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TrialStatusMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        // Arrange
        TrialStatusDb db = DomainBuilderDatabase.getTrialStatusDb();

        // Act
        TrialStatus domain = mapper.toModel(db);

        // Assert
        assertNotNull(domain);
        assertEquals(db.getExtid(), domain.getExtid());
        assertEquals(db.getTrialId(), domain.getTrialId());
        assertEquals(db.getPatientId(), domain.getPatientId());
        assertEquals(db.getStatus(), domain.getStatus());
        assertEquals(db.getNotes(), domain.getNotes());
        assertEquals(db.getStatusChangedAt(), domain.getStatusChangedAt());
        assertEquals(db.getCreatedAt(), domain.getCreatedAt());
        assertEquals(db.getUpdatedAt(), domain.getUpdatedAt());
        assertEquals(db.getDeletedAt(), domain.getDeletedAt());
        assertEquals(db.getActive(), domain.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        // Arrange
        TrialStatus domain = DomainBuilderDatabase.getTrialStatus();

        // Act
        TrialStatusDb db = mapper.toDb(domain);

        // Assert
        assertNotNull(db);
        assertEquals(domain.getExtid(), db.getExtid());
        assertEquals(domain.getTrialId(), db.getTrialId());
        assertEquals(domain.getPatientId(), db.getPatientId());
        assertEquals(domain.getStatus(), db.getStatus());
        assertEquals(domain.getNotes(), db.getNotes());
        assertEquals(domain.getStatusChangedAt(), db.getStatusChangedAt());
        assertEquals(domain.getCreatedAt(), db.getCreatedAt());
        assertEquals(domain.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(domain.getDeletedAt(), db.getDeletedAt());
        assertEquals(domain.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        // Arrange
        TrialStatusDb db1 = DomainBuilderDatabase.getTrialStatusDb();
        TrialStatusDb db2 = DomainBuilderDatabase.getTrialStatusDb();
        List<TrialStatusDb> dbList = Arrays.asList(db1, db2);

        // Act
        List<TrialStatus> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals(db1.getExtid(), domainList.get(0).getExtid());
        assertEquals(db2.getExtid(), domainList.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        // Arrange
        List<TrialStatusDb> dbList = Arrays.asList();

        // Act
        List<TrialStatus> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        // Act
        List<TrialStatus> domainList = mapper.toModelList(null);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toDbList_shouldMapAllItems() {
        // Arrange
        TrialStatus domain1 = DomainBuilderDatabase.getTrialStatus();
        TrialStatus domain2 = DomainBuilderDatabase.getTrialStatus();
        List<TrialStatus> domainList = Arrays.asList(domain1, domain2);

        // Act
        List<TrialStatusDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(2, dbList.size());
        assertEquals(domain1.getExtid(), dbList.get(0).getExtid());
        assertEquals(domain2.getExtid(), dbList.get(1).getExtid());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        // Arrange
        List<TrialStatus> domainList = Arrays.asList();

        // Act
        List<TrialStatusDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        // Act
        List<TrialStatusDb> dbList = mapper.toDbList(null);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }
}
