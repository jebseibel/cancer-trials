package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.Condition;
import com.seibel.cancer.database.db.entity.ConditionDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConditionMapperTest {

    private ConditionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ConditionMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        // Arrange
        ConditionDb db = DomainBuilderDatabase.getConditionDb();

        // Act
        Condition domain = mapper.toModel(db);

        // Assert
        assertNotNull(domain);
        assertEquals(db.getExtid(), domain.getExtid());
        assertEquals(db.getName(), domain.getName());
        assertEquals(db.getCreatedAt(), domain.getCreatedAt());
        assertEquals(db.getUpdatedAt(), domain.getUpdatedAt());
        assertEquals(db.getDeletedAt(), domain.getDeletedAt());
        assertEquals(db.getActive(), domain.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        // Arrange
        Condition domain = DomainBuilderDatabase.getCondition();

        // Act
        ConditionDb db = mapper.toDb(domain);

        // Assert
        assertNotNull(db);
        assertEquals(domain.getExtid(), db.getExtid());
        assertEquals(domain.getName(), db.getName());
        assertEquals(domain.getCreatedAt(), db.getCreatedAt());
        assertEquals(domain.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(domain.getDeletedAt(), db.getDeletedAt());
        assertEquals(domain.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        // Arrange
        ConditionDb db1 = DomainBuilderDatabase.getConditionDb();
        ConditionDb db2 = DomainBuilderDatabase.getConditionDb();
        List<ConditionDb> dbList = Arrays.asList(db1, db2);

        // Act
        List<Condition> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals(db1.getExtid(), domainList.get(0).getExtid());
        assertEquals(db2.getExtid(), domainList.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        // Arrange
        List<ConditionDb> dbList = Arrays.asList();

        // Act
        List<Condition> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        // Act
        List<Condition> domainList = mapper.toModelList(null);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toDbList_shouldMapAllItems() {
        // Arrange
        Condition domain1 = DomainBuilderDatabase.getCondition();
        Condition domain2 = DomainBuilderDatabase.getCondition();
        List<Condition> domainList = Arrays.asList(domain1, domain2);

        // Act
        List<ConditionDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(2, dbList.size());
        assertEquals(domain1.getExtid(), dbList.get(0).getExtid());
        assertEquals(domain2.getExtid(), dbList.get(1).getExtid());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        // Arrange
        List<Condition> domainList = Arrays.asList();

        // Act
        List<ConditionDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        // Act
        List<ConditionDb> dbList = mapper.toDbList(null);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }
}
