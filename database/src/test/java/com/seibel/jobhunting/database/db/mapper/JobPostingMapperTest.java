package com.seibel.jobhunting.database.db.mapper;

import com.seibel.jobhunting.common.domain.JobPosting;
import com.seibel.jobhunting.database.db.entity.JobPostingDb;
import com.seibel.jobhunting.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class JobPostingMapperTest {

    private JobPostingMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new JobPostingMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        // Arrange
        JobPostingDb db = DomainBuilderDatabase.getJobPostingDb();

        // Act
        JobPosting domain = mapper.toModel(db);

        // Assert
        assertNotNull(domain);
        assertEquals(db.getExtid(), domain.getExtid());
        assertEquals(db.getTitle(), domain.getTitle());
        assertEquals(db.getCompanyId(), domain.getCompanyId());
        assertEquals(db.getDescription(), domain.getDescription());
        assertEquals(db.getCity(), domain.getCity());
        assertEquals(db.getState(), domain.getState());
        assertEquals(db.getCountry(), domain.getCountry());
        assertEquals(db.getWorkMode(), domain.getWorkMode());
        assertEquals(db.getSalaryMin(), domain.getSalaryMin());
        assertEquals(db.getSalaryMax(), domain.getSalaryMax());
        assertEquals(db.getSalaryCurrency(), domain.getSalaryCurrency());
        assertEquals(db.getSource(), domain.getSource());
        assertEquals(db.getSourceUrl(), domain.getSourceUrl());
        assertEquals(db.getPostedAt(), domain.getPostedAt());
        assertEquals(db.getStatus(), domain.getStatus());
        assertEquals(db.getNotes(), domain.getNotes());
        assertEquals(db.getCreatedAt(), domain.getCreatedAt());
        assertEquals(db.getUpdatedAt(), domain.getUpdatedAt());
        assertEquals(db.getDeletedAt(), domain.getDeletedAt());
        assertEquals(db.getActive(), domain.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        // Arrange
        JobPosting domain = DomainBuilderDatabase.getJobPosting();

        // Act
        JobPostingDb db = mapper.toDb(domain);

        // Assert
        assertNotNull(db);
        assertEquals(domain.getExtid(), db.getExtid());
        assertEquals(domain.getTitle(), db.getTitle());
        assertEquals(domain.getCompanyId(), db.getCompanyId());
        assertEquals(domain.getDescription(), db.getDescription());
        assertEquals(domain.getCity(), db.getCity());
        assertEquals(domain.getState(), db.getState());
        assertEquals(domain.getCountry(), db.getCountry());
        assertEquals(domain.getWorkMode(), db.getWorkMode());
        assertEquals(domain.getSalaryMin(), db.getSalaryMin());
        assertEquals(domain.getSalaryMax(), db.getSalaryMax());
        assertEquals(domain.getSalaryCurrency(), db.getSalaryCurrency());
        assertEquals(domain.getSource(), db.getSource());
        assertEquals(domain.getSourceUrl(), db.getSourceUrl());
        assertEquals(domain.getPostedAt(), db.getPostedAt());
        assertEquals(domain.getStatus(), db.getStatus());
        assertEquals(domain.getNotes(), db.getNotes());
        assertEquals(domain.getCreatedAt(), db.getCreatedAt());
        assertEquals(domain.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(domain.getDeletedAt(), db.getDeletedAt());
        assertEquals(domain.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        // Arrange
        JobPostingDb db1 = DomainBuilderDatabase.getJobPostingDb();
        JobPostingDb db2 = DomainBuilderDatabase.getJobPostingDb();
        List<JobPostingDb> dbList = Arrays.asList(db1, db2);

        // Act
        List<JobPosting> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals(db1.getExtid(), domainList.get(0).getExtid());
        assertEquals(db2.getExtid(), domainList.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        // Arrange
        List<JobPostingDb> dbList = Arrays.asList();

        // Act
        List<JobPosting> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        // Act
        List<JobPosting> domainList = mapper.toModelList(null);

        // Assert
        assertNull(domainList);
    }

    @Test
    void toDbList_shouldMapAllItems() {
        // Arrange
        JobPosting domain1 = DomainBuilderDatabase.getJobPosting();
        JobPosting domain2 = DomainBuilderDatabase.getJobPosting();
        List<JobPosting> domainList = Arrays.asList(domain1, domain2);

        // Act
        List<JobPostingDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(2, dbList.size());
        assertEquals(domain1.getExtid(), dbList.get(0).getExtid());
        assertEquals(domain2.getExtid(), dbList.get(1).getExtid());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        // Arrange
        List<JobPosting> domainList = Arrays.asList();

        // Act
        List<JobPostingDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        // Act
        List<JobPostingDb> dbList = mapper.toDbList(null);

        // Assert
        assertNull(dbList);
    }
}
