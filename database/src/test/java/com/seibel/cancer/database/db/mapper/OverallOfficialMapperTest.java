package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.OverallOfficial;
import com.seibel.cancer.database.db.entity.OverallOfficialDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class OverallOfficialMapperTest {

    private OverallOfficialMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new OverallOfficialMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        // Arrange
        OverallOfficialDb db = DomainBuilderDatabase.getOverallOfficialDb();

        // Act
        OverallOfficial domain = mapper.toModel(db);

        // Assert
        assertNotNull(domain);
        assertEquals(db.getExtid(), domain.getExtid());
        assertEquals(db.getTrialId(), domain.getTrialId());
        assertEquals(db.getName(), domain.getName());
        assertEquals(db.getAffiliation(), domain.getAffiliation());
        assertEquals(db.getRole(), domain.getRole());
        assertEquals(db.getCreatedAt(), domain.getCreatedAt());
        assertEquals(db.getUpdatedAt(), domain.getUpdatedAt());
        assertEquals(db.getDeletedAt(), domain.getDeletedAt());
        assertEquals(db.getActive(), domain.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        // Arrange
        OverallOfficial domain = DomainBuilderDatabase.getOverallOfficial();

        // Act
        OverallOfficialDb db = mapper.toDb(domain);

        // Assert
        assertNotNull(db);
        assertEquals(domain.getExtid(), db.getExtid());
        assertEquals(domain.getTrialId(), db.getTrialId());
        assertEquals(domain.getName(), db.getName());
        assertEquals(domain.getAffiliation(), db.getAffiliation());
        assertEquals(domain.getRole(), db.getRole());
        assertEquals(domain.getCreatedAt(), db.getCreatedAt());
        assertEquals(domain.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(domain.getDeletedAt(), db.getDeletedAt());
        assertEquals(domain.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        // Arrange
        OverallOfficialDb db1 = DomainBuilderDatabase.getOverallOfficialDb();
        OverallOfficialDb db2 = DomainBuilderDatabase.getOverallOfficialDb();
        List<OverallOfficialDb> dbList = Arrays.asList(db1, db2);

        // Act
        List<OverallOfficial> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals(db1.getExtid(), domainList.get(0).getExtid());
        assertEquals(db2.getExtid(), domainList.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        // Arrange
        List<OverallOfficialDb> dbList = Arrays.asList();

        // Act
        List<OverallOfficial> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        // Act
        List<OverallOfficial> domainList = mapper.toModelList(null);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toDbList_shouldMapAllItems() {
        // Arrange
        OverallOfficial domain1 = DomainBuilderDatabase.getOverallOfficial();
        OverallOfficial domain2 = DomainBuilderDatabase.getOverallOfficial();
        List<OverallOfficial> domainList = Arrays.asList(domain1, domain2);

        // Act
        List<OverallOfficialDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(2, dbList.size());
        assertEquals(domain1.getExtid(), dbList.get(0).getExtid());
        assertEquals(domain2.getExtid(), dbList.get(1).getExtid());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        // Arrange
        List<OverallOfficial> domainList = Arrays.asList();

        // Act
        List<OverallOfficialDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        // Act
        List<OverallOfficialDb> dbList = mapper.toDbList(null);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }
}
