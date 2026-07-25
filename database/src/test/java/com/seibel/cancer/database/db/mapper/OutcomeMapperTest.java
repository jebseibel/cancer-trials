package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.Outcome;
import com.seibel.cancer.database.db.entity.OutcomeDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OutcomeMapperTest {

    private OutcomeMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new OutcomeMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        // Arrange
        OutcomeDb db = DomainBuilderDatabase.getOutcomeDb();

        // Act
        Outcome domain = mapper.toModel(db);

        // Assert
        assertNotNull(domain);
        assertEquals(db.getExtid(), domain.getExtid());
        assertEquals(db.getTrialId(), domain.getTrialId());
        assertEquals(db.getOutcomeType(), domain.getOutcomeType());
        assertEquals(db.getMeasure(), domain.getMeasure());
        assertEquals(db.getDescription(), domain.getDescription());
        assertEquals(db.getTimeFrame(), domain.getTimeFrame());
        assertEquals(db.getCreatedAt(), domain.getCreatedAt());
        assertEquals(db.getUpdatedAt(), domain.getUpdatedAt());
        assertEquals(db.getDeletedAt(), domain.getDeletedAt());
        assertEquals(db.getActive(), domain.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        // Arrange
        Outcome domain = DomainBuilderDatabase.getOutcome();

        // Act
        OutcomeDb db = mapper.toDb(domain);

        // Assert
        assertNotNull(db);
        assertEquals(domain.getExtid(), db.getExtid());
        assertEquals(domain.getTrialId(), db.getTrialId());
        assertEquals(domain.getOutcomeType(), db.getOutcomeType());
        assertEquals(domain.getMeasure(), db.getMeasure());
        assertEquals(domain.getDescription(), db.getDescription());
        assertEquals(domain.getTimeFrame(), db.getTimeFrame());
        assertEquals(domain.getCreatedAt(), db.getCreatedAt());
        assertEquals(domain.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(domain.getDeletedAt(), db.getDeletedAt());
        assertEquals(domain.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        // Arrange
        OutcomeDb db1 = DomainBuilderDatabase.getOutcomeDb();
        OutcomeDb db2 = DomainBuilderDatabase.getOutcomeDb();
        List<OutcomeDb> dbList = Arrays.asList(db1, db2);

        // Act
        List<Outcome> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals(db1.getExtid(), domainList.get(0).getExtid());
        assertEquals(db2.getExtid(), domainList.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        // Arrange
        List<OutcomeDb> dbList = Arrays.asList();

        // Act
        List<Outcome> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        // Act
        List<Outcome> domainList = mapper.toModelList(null);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toDbList_shouldMapAllItems() {
        // Arrange
        Outcome domain1 = DomainBuilderDatabase.getOutcome();
        Outcome domain2 = DomainBuilderDatabase.getOutcome();
        List<Outcome> domainList = Arrays.asList(domain1, domain2);

        // Act
        List<OutcomeDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(2, dbList.size());
        assertEquals(domain1.getExtid(), dbList.get(0).getExtid());
        assertEquals(domain2.getExtid(), dbList.get(1).getExtid());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        // Arrange
        List<Outcome> domainList = Arrays.asList();

        // Act
        List<OutcomeDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        // Act
        List<OutcomeDb> dbList = mapper.toDbList(null);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }
}
