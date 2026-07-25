package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.StagingRawTrial;
import com.seibel.cancer.database.db.entity.StagingRawTrialDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StagingRawTrialMapperTest {

    private StagingRawTrialMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new StagingRawTrialMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        // Arrange
        StagingRawTrialDb db = DomainBuilderDatabase.getStagingRawTrialDb();

        // Act
        StagingRawTrial domain = mapper.toModel(db);

        // Assert
        assertNotNull(domain);
        assertEquals(db.getExtid(), domain.getExtid());
        assertEquals(db.getTrialSourceId(), domain.getTrialSourceId());
        assertEquals(db.getSourceTrialId(), domain.getSourceTrialId());
        assertEquals(db.getRawPayload(), domain.getRawPayload());
        assertEquals(db.getFetchedAt(), domain.getFetchedAt());
        assertEquals(db.getNormalizedAt(), domain.getNormalizedAt());
        assertEquals(db.getNormalizationError(), domain.getNormalizationError());
        assertEquals(db.getCreatedAt(), domain.getCreatedAt());
        assertEquals(db.getUpdatedAt(), domain.getUpdatedAt());
        assertEquals(db.getDeletedAt(), domain.getDeletedAt());
        assertEquals(db.getActive(), domain.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        // Arrange
        StagingRawTrial domain = DomainBuilderDatabase.getStagingRawTrial();

        // Act
        StagingRawTrialDb db = mapper.toDb(domain);

        // Assert
        assertNotNull(db);
        assertEquals(domain.getExtid(), db.getExtid());
        assertEquals(domain.getTrialSourceId(), db.getTrialSourceId());
        assertEquals(domain.getSourceTrialId(), db.getSourceTrialId());
        assertEquals(domain.getRawPayload(), db.getRawPayload());
        assertEquals(domain.getFetchedAt(), db.getFetchedAt());
        assertEquals(domain.getNormalizedAt(), db.getNormalizedAt());
        assertEquals(domain.getNormalizationError(), db.getNormalizationError());
        assertEquals(domain.getCreatedAt(), db.getCreatedAt());
        assertEquals(domain.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(domain.getDeletedAt(), db.getDeletedAt());
        assertEquals(domain.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        // Arrange
        StagingRawTrialDb db1 = DomainBuilderDatabase.getStagingRawTrialDb();
        StagingRawTrialDb db2 = DomainBuilderDatabase.getStagingRawTrialDb();
        List<StagingRawTrialDb> dbList = Arrays.asList(db1, db2);

        // Act
        List<StagingRawTrial> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals(db1.getExtid(), domainList.get(0).getExtid());
        assertEquals(db2.getExtid(), domainList.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        // Arrange
        List<StagingRawTrialDb> dbList = Arrays.asList();

        // Act
        List<StagingRawTrial> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        // Act
        List<StagingRawTrial> domainList = mapper.toModelList(null);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toDbList_shouldMapAllItems() {
        // Arrange
        StagingRawTrial domain1 = DomainBuilderDatabase.getStagingRawTrial();
        StagingRawTrial domain2 = DomainBuilderDatabase.getStagingRawTrial();
        List<StagingRawTrial> domainList = Arrays.asList(domain1, domain2);

        // Act
        List<StagingRawTrialDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(2, dbList.size());
        assertEquals(domain1.getExtid(), dbList.get(0).getExtid());
        assertEquals(domain2.getExtid(), dbList.get(1).getExtid());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        // Arrange
        List<StagingRawTrial> domainList = Arrays.asList();

        // Act
        List<StagingRawTrialDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        // Act
        List<StagingRawTrialDb> dbList = mapper.toDbList(null);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }
}
