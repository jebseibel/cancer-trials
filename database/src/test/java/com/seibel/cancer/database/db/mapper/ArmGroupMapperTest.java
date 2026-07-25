package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.ArmGroup;
import com.seibel.cancer.database.db.entity.ArmGroupDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ArmGroupMapperTest {

    private ArmGroupMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ArmGroupMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        // Arrange
        ArmGroupDb db = DomainBuilderDatabase.getArmGroupDb();

        // Act
        ArmGroup domain = mapper.toModel(db);

        // Assert
        assertNotNull(domain);
        assertEquals(db.getExtid(), domain.getExtid());
        assertEquals(db.getTrialId(), domain.getTrialId());
        assertEquals(db.getLabel(), domain.getLabel());
        assertEquals(db.getType(), domain.getType());
        assertEquals(db.getDescription(), domain.getDescription());
        assertEquals(db.getCreatedAt(), domain.getCreatedAt());
        assertEquals(db.getUpdatedAt(), domain.getUpdatedAt());
        assertEquals(db.getDeletedAt(), domain.getDeletedAt());
        assertEquals(db.getActive(), domain.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        // Arrange
        ArmGroup domain = DomainBuilderDatabase.getArmGroup();

        // Act
        ArmGroupDb db = mapper.toDb(domain);

        // Assert
        assertNotNull(db);
        assertEquals(domain.getExtid(), db.getExtid());
        assertEquals(domain.getTrialId(), db.getTrialId());
        assertEquals(domain.getLabel(), db.getLabel());
        assertEquals(domain.getType(), db.getType());
        assertEquals(domain.getDescription(), db.getDescription());
        assertEquals(domain.getCreatedAt(), db.getCreatedAt());
        assertEquals(domain.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(domain.getDeletedAt(), db.getDeletedAt());
        assertEquals(domain.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        // Arrange
        ArmGroupDb db1 = DomainBuilderDatabase.getArmGroupDb();
        ArmGroupDb db2 = DomainBuilderDatabase.getArmGroupDb();
        List<ArmGroupDb> dbList = Arrays.asList(db1, db2);

        // Act
        List<ArmGroup> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals(db1.getExtid(), domainList.get(0).getExtid());
        assertEquals(db2.getExtid(), domainList.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        // Arrange
        List<ArmGroupDb> dbList = Arrays.asList();

        // Act
        List<ArmGroup> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        // Act
        List<ArmGroup> domainList = mapper.toModelList(null);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toDbList_shouldMapAllItems() {
        // Arrange
        ArmGroup domain1 = DomainBuilderDatabase.getArmGroup();
        ArmGroup domain2 = DomainBuilderDatabase.getArmGroup();
        List<ArmGroup> domainList = Arrays.asList(domain1, domain2);

        // Act
        List<ArmGroupDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(2, dbList.size());
        assertEquals(domain1.getExtid(), dbList.get(0).getExtid());
        assertEquals(domain2.getExtid(), dbList.get(1).getExtid());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        // Arrange
        List<ArmGroup> domainList = Arrays.asList();

        // Act
        List<ArmGroupDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        // Act
        List<ArmGroupDb> dbList = mapper.toDbList(null);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }
}
