package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.TrialSource;
import com.seibel.cancer.database.db.entity.TrialSourceDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TrialSourceMapperTest {

    private TrialSourceMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TrialSourceMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        // Arrange
        TrialSourceDb db = DomainBuilderDatabase.getTrialSourceDb();

        // Act
        TrialSource domain = mapper.toModel(db);

        // Assert
        assertNotNull(domain);
        assertEquals(db.getExtid(), domain.getExtid());
        assertEquals(db.getCode(), domain.getCode());
        assertEquals(db.getName(), domain.getName());
        assertEquals(db.getBaseUrl(), domain.getBaseUrl());
        assertEquals(db.getCreatedAt(), domain.getCreatedAt());
        assertEquals(db.getUpdatedAt(), domain.getUpdatedAt());
        assertEquals(db.getDeletedAt(), domain.getDeletedAt());
        assertEquals(db.getActive(), domain.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        // Arrange
        TrialSource domain = DomainBuilderDatabase.getTrialSource();

        // Act
        TrialSourceDb db = mapper.toDb(domain);

        // Assert
        assertNotNull(db);
        assertEquals(domain.getExtid(), db.getExtid());
        assertEquals(domain.getCode(), db.getCode());
        assertEquals(domain.getName(), db.getName());
        assertEquals(domain.getBaseUrl(), db.getBaseUrl());
        assertEquals(domain.getCreatedAt(), db.getCreatedAt());
        assertEquals(domain.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(domain.getDeletedAt(), db.getDeletedAt());
        assertEquals(domain.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        // Arrange
        TrialSourceDb db1 = DomainBuilderDatabase.getTrialSourceDb();
        TrialSourceDb db2 = DomainBuilderDatabase.getTrialSourceDb();
        List<TrialSourceDb> dbList = Arrays.asList(db1, db2);

        // Act
        List<TrialSource> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals(db1.getExtid(), domainList.get(0).getExtid());
        assertEquals(db2.getExtid(), domainList.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        // Arrange
        List<TrialSourceDb> dbList = Arrays.asList();

        // Act
        List<TrialSource> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        // Act
        List<TrialSource> domainList = mapper.toModelList(null);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toDbList_shouldMapAllItems() {
        // Arrange
        TrialSource domain1 = DomainBuilderDatabase.getTrialSource();
        TrialSource domain2 = DomainBuilderDatabase.getTrialSource();
        List<TrialSource> domainList = Arrays.asList(domain1, domain2);

        // Act
        List<TrialSourceDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(2, dbList.size());
        assertEquals(domain1.getExtid(), dbList.get(0).getExtid());
        assertEquals(domain2.getExtid(), dbList.get(1).getExtid());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        // Arrange
        List<TrialSource> domainList = Arrays.asList();

        // Act
        List<TrialSourceDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        // Act
        List<TrialSourceDb> dbList = mapper.toDbList(null);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }
}
