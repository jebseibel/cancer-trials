package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.Intervention;
import com.seibel.cancer.database.db.entity.InterventionDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class InterventionMapperTest {

    private InterventionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new InterventionMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        // Arrange
        InterventionDb db = DomainBuilderDatabase.getInterventionDb();

        // Act
        Intervention domain = mapper.toModel(db);

        // Assert
        assertNotNull(domain);
        assertEquals(db.getExtid(), domain.getExtid());
        assertEquals(db.getTrialId(), domain.getTrialId());
        assertEquals(db.getType(), domain.getType());
        assertEquals(db.getName(), domain.getName());
        assertEquals(db.getDescription(), domain.getDescription());
        assertEquals(db.getCreatedAt(), domain.getCreatedAt());
        assertEquals(db.getUpdatedAt(), domain.getUpdatedAt());
        assertEquals(db.getDeletedAt(), domain.getDeletedAt());
        assertEquals(db.getActive(), domain.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        // Arrange
        Intervention domain = DomainBuilderDatabase.getIntervention();

        // Act
        InterventionDb db = mapper.toDb(domain);

        // Assert
        assertNotNull(db);
        assertEquals(domain.getExtid(), db.getExtid());
        assertEquals(domain.getTrialId(), db.getTrialId());
        assertEquals(domain.getType(), db.getType());
        assertEquals(domain.getName(), db.getName());
        assertEquals(domain.getDescription(), db.getDescription());
        assertEquals(domain.getCreatedAt(), db.getCreatedAt());
        assertEquals(domain.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(domain.getDeletedAt(), db.getDeletedAt());
        assertEquals(domain.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        // Arrange
        InterventionDb db1 = DomainBuilderDatabase.getInterventionDb();
        InterventionDb db2 = DomainBuilderDatabase.getInterventionDb();
        List<InterventionDb> dbList = Arrays.asList(db1, db2);

        // Act
        List<Intervention> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals(db1.getExtid(), domainList.get(0).getExtid());
        assertEquals(db2.getExtid(), domainList.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        // Arrange
        List<InterventionDb> dbList = Arrays.asList();

        // Act
        List<Intervention> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        // Act
        List<Intervention> domainList = mapper.toModelList(null);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toDbList_shouldMapAllItems() {
        // Arrange
        Intervention domain1 = DomainBuilderDatabase.getIntervention();
        Intervention domain2 = DomainBuilderDatabase.getIntervention();
        List<Intervention> domainList = Arrays.asList(domain1, domain2);

        // Act
        List<InterventionDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(2, dbList.size());
        assertEquals(domain1.getExtid(), dbList.get(0).getExtid());
        assertEquals(domain2.getExtid(), dbList.get(1).getExtid());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        // Arrange
        List<Intervention> domainList = Arrays.asList();

        // Act
        List<InterventionDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        // Act
        List<InterventionDb> dbList = mapper.toDbList(null);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }
}
