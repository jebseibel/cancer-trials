package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.Sponsor;
import com.seibel.cancer.database.db.entity.SponsorDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SponsorMapperTest {

    private SponsorMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new SponsorMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        // Arrange
        SponsorDb db = DomainBuilderDatabase.getSponsorDb();

        // Act
        Sponsor domain = mapper.toModel(db);

        // Assert
        assertNotNull(domain);
        assertEquals(db.getExtid(), domain.getExtid());
        assertEquals(db.getName(), domain.getName());
        assertEquals(db.getOrgClass(), domain.getOrgClass());
        assertEquals(db.getCreatedAt(), domain.getCreatedAt());
        assertEquals(db.getUpdatedAt(), domain.getUpdatedAt());
        assertEquals(db.getDeletedAt(), domain.getDeletedAt());
        assertEquals(db.getActive(), domain.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        // Arrange
        Sponsor domain = DomainBuilderDatabase.getSponsor();

        // Act
        SponsorDb db = mapper.toDb(domain);

        // Assert
        assertNotNull(db);
        assertEquals(domain.getExtid(), db.getExtid());
        assertEquals(domain.getName(), db.getName());
        assertEquals(domain.getOrgClass(), db.getOrgClass());
        assertEquals(domain.getCreatedAt(), db.getCreatedAt());
        assertEquals(domain.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(domain.getDeletedAt(), db.getDeletedAt());
        assertEquals(domain.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        // Arrange
        SponsorDb db1 = DomainBuilderDatabase.getSponsorDb();
        SponsorDb db2 = DomainBuilderDatabase.getSponsorDb();
        List<SponsorDb> dbList = Arrays.asList(db1, db2);

        // Act
        List<Sponsor> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals(db1.getExtid(), domainList.get(0).getExtid());
        assertEquals(db2.getExtid(), domainList.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        // Arrange
        List<SponsorDb> dbList = Arrays.asList();

        // Act
        List<Sponsor> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        // Act
        List<Sponsor> domainList = mapper.toModelList(null);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toDbList_shouldMapAllItems() {
        // Arrange
        Sponsor domain1 = DomainBuilderDatabase.getSponsor();
        Sponsor domain2 = DomainBuilderDatabase.getSponsor();
        List<Sponsor> domainList = Arrays.asList(domain1, domain2);

        // Act
        List<SponsorDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(2, dbList.size());
        assertEquals(domain1.getExtid(), dbList.get(0).getExtid());
        assertEquals(domain2.getExtid(), dbList.get(1).getExtid());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        // Arrange
        List<Sponsor> domainList = Arrays.asList();

        // Act
        List<SponsorDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        // Act
        List<SponsorDb> dbList = mapper.toDbList(null);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }
}
