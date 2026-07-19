package com.seibel.jobs.database.db.mapper;

import com.seibel.jobs.common.domain.Company;
import com.seibel.jobs.database.db.entity.CompanyDb;
import com.seibel.jobs.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CompanyMapperTest {

    private CompanyMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new CompanyMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        // Arrange
        CompanyDb db = DomainBuilderDatabase.getCompanyDb();

        // Act
        Company domain = mapper.toModel(db);

        // Assert
        assertNotNull(domain);
        assertEquals(db.getExtid(), domain.getExtid());
        assertEquals(db.getName(), domain.getName());
        assertEquals(db.getWebsite(), domain.getWebsite());
        assertEquals(db.getIndustry(), domain.getIndustry());
        assertEquals(db.getNotes(), domain.getNotes());
        assertEquals(db.getCreatedAt(), domain.getCreatedAt());
        assertEquals(db.getUpdatedAt(), domain.getUpdatedAt());
        assertEquals(db.getDeletedAt(), domain.getDeletedAt());
        assertEquals(db.getActive(), domain.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        // Arrange
        Company domain = DomainBuilderDatabase.getCompany();

        // Act
        CompanyDb db = mapper.toDb(domain);

        // Assert
        assertNotNull(db);
        assertEquals(domain.getExtid(), db.getExtid());
        assertEquals(domain.getName(), db.getName());
        assertEquals(domain.getWebsite(), db.getWebsite());
        assertEquals(domain.getIndustry(), db.getIndustry());
        assertEquals(domain.getNotes(), db.getNotes());
        assertEquals(domain.getCreatedAt(), db.getCreatedAt());
        assertEquals(domain.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(domain.getDeletedAt(), db.getDeletedAt());
        assertEquals(domain.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        // Arrange
        CompanyDb db1 = DomainBuilderDatabase.getCompanyDb();
        CompanyDb db2 = DomainBuilderDatabase.getCompanyDb();
        List<CompanyDb> dbList = Arrays.asList(db1, db2);

        // Act
        List<Company> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals(db1.getExtid(), domainList.get(0).getExtid());
        assertEquals(db2.getExtid(), domainList.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        // Arrange
        List<CompanyDb> dbList = Arrays.asList();

        // Act
        List<Company> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        // Act
        List<Company> domainList = mapper.toModelList(null);

        // Assert
        assertNull(domainList);
    }

    @Test
    void toDbList_shouldMapAllItems() {
        // Arrange
        Company domain1 = DomainBuilderDatabase.getCompany();
        Company domain2 = DomainBuilderDatabase.getCompany();
        List<Company> domainList = Arrays.asList(domain1, domain2);

        // Act
        List<CompanyDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(2, dbList.size());
        assertEquals(domain1.getExtid(), dbList.get(0).getExtid());
        assertEquals(domain2.getExtid(), dbList.get(1).getExtid());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        // Arrange
        List<Company> domainList = Arrays.asList();

        // Act
        List<CompanyDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        // Act
        List<CompanyDb> dbList = mapper.toDbList(null);

        // Assert
        assertNull(dbList);
    }
}
