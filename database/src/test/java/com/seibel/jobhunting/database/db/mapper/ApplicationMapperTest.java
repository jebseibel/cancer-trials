package com.seibel.jobhunting.database.db.mapper;

import com.seibel.jobhunting.common.domain.Application;
import com.seibel.jobhunting.database.db.entity.ApplicationDb;
import com.seibel.jobhunting.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApplicationMapperTest {

    private ApplicationMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ApplicationMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        // Arrange
        ApplicationDb db = DomainBuilderDatabase.getApplicationDb();

        // Act
        Application domain = mapper.toModel(db);

        // Assert
        assertNotNull(domain);
        assertEquals(db.getExtid(), domain.getExtid());
        assertEquals(db.getJobPostingId(), domain.getJobPostingId());
        assertEquals(db.getDateApplied(), domain.getDateApplied());
        assertEquals(db.getResumeVersion(), domain.getResumeVersion());
        assertEquals(db.getApplicationStatus(), domain.getApplicationStatus());
        assertEquals(db.getNotes(), domain.getNotes());
        assertEquals(db.getCreatedAt(), domain.getCreatedAt());
        assertEquals(db.getUpdatedAt(), domain.getUpdatedAt());
        assertEquals(db.getDeletedAt(), domain.getDeletedAt());
        assertEquals(db.getActive(), domain.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        // Arrange
        Application domain = DomainBuilderDatabase.getApplication();

        // Act
        ApplicationDb db = mapper.toDb(domain);

        // Assert
        assertNotNull(db);
        assertEquals(domain.getExtid(), db.getExtid());
        assertEquals(domain.getJobPostingId(), db.getJobPostingId());
        assertEquals(domain.getDateApplied(), db.getDateApplied());
        assertEquals(domain.getResumeVersion(), db.getResumeVersion());
        assertEquals(domain.getApplicationStatus(), db.getApplicationStatus());
        assertEquals(domain.getNotes(), db.getNotes());
        assertEquals(domain.getCreatedAt(), db.getCreatedAt());
        assertEquals(domain.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(domain.getDeletedAt(), db.getDeletedAt());
        assertEquals(domain.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        // Arrange
        ApplicationDb db1 = DomainBuilderDatabase.getApplicationDb();
        ApplicationDb db2 = DomainBuilderDatabase.getApplicationDb();
        List<ApplicationDb> dbList = Arrays.asList(db1, db2);

        // Act
        List<Application> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals(db1.getExtid(), domainList.get(0).getExtid());
        assertEquals(db2.getExtid(), domainList.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        // Arrange
        List<ApplicationDb> dbList = Arrays.asList();

        // Act
        List<Application> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        // Act
        List<Application> domainList = mapper.toModelList(null);

        // Assert
        assertNull(domainList);
    }

    @Test
    void toDbList_shouldMapAllItems() {
        // Arrange
        Application domain1 = DomainBuilderDatabase.getApplication();
        Application domain2 = DomainBuilderDatabase.getApplication();
        List<Application> domainList = Arrays.asList(domain1, domain2);

        // Act
        List<ApplicationDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(2, dbList.size());
        assertEquals(domain1.getExtid(), dbList.get(0).getExtid());
        assertEquals(domain2.getExtid(), dbList.get(1).getExtid());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        // Arrange
        List<Application> domainList = Arrays.asList();

        // Act
        List<ApplicationDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        // Act
        List<ApplicationDb> dbList = mapper.toDbList(null);

        // Assert
        assertNull(dbList);
    }
}
