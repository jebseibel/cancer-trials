package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.database.db.entity.TrialDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TrialMapperTest {

    private TrialMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TrialMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        // Arrange
        TrialDb db = DomainBuilderDatabase.getTrialDb();

        // Act
        Trial domain = mapper.toModel(db);

        // Assert
        assertNotNull(domain);
        assertEquals(db.getExtid(), domain.getExtid());
        assertEquals(db.getNctId(), domain.getNctId());
        assertEquals(db.getBriefTitle(), domain.getBriefTitle());
        assertEquals(db.getOfficialTitle(), domain.getOfficialTitle());
        assertEquals(db.getOverallStatus(), domain.getOverallStatus());
        assertEquals(db.getStudyType(), domain.getStudyType());
        assertEquals(db.getBriefSummary(), domain.getBriefSummary());
        assertEquals(db.getDetailedDescription(), domain.getDetailedDescription());
        assertEquals(db.getStartDate(), domain.getStartDate());
        assertEquals(db.getPrimaryCompletionDate(), domain.getPrimaryCompletionDate());
        assertEquals(db.getCompletionDate(), domain.getCompletionDate());
        assertEquals(db.getLastUpdatePostedDate(), domain.getLastUpdatePostedDate());
        assertEquals(db.getEnrollmentCount(), domain.getEnrollmentCount());
        assertEquals(db.getEnrollmentType(), domain.getEnrollmentType());
        assertEquals(db.getHealthyVolunteers(), domain.getHealthyVolunteers());
        assertEquals(db.getSex(), domain.getSex());
        assertEquals(db.getMinimumAge(), domain.getMinimumAge());
        assertEquals(db.getMaximumAge(), domain.getMaximumAge());
        assertEquals(db.getEligibilityCriteria(), domain.getEligibilityCriteria());
        assertEquals(db.getIsPaidStudy(), domain.getIsPaidStudy());
        assertEquals(db.getPaidAmount(), domain.getPaidAmount());
        assertEquals(db.getCreatedAt(), domain.getCreatedAt());
        assertEquals(db.getUpdatedAt(), domain.getUpdatedAt());
        assertEquals(db.getDeletedAt(), domain.getDeletedAt());
        assertEquals(db.getActive(), domain.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        // Arrange
        Trial domain = DomainBuilderDatabase.getTrial();

        // Act
        TrialDb db = mapper.toDb(domain);

        // Assert
        assertNotNull(db);
        assertEquals(domain.getExtid(), db.getExtid());
        assertEquals(domain.getNctId(), db.getNctId());
        assertEquals(domain.getBriefTitle(), db.getBriefTitle());
        assertEquals(domain.getOfficialTitle(), db.getOfficialTitle());
        assertEquals(domain.getOverallStatus(), db.getOverallStatus());
        assertEquals(domain.getStudyType(), db.getStudyType());
        assertEquals(domain.getBriefSummary(), db.getBriefSummary());
        assertEquals(domain.getDetailedDescription(), db.getDetailedDescription());
        assertEquals(domain.getStartDate(), db.getStartDate());
        assertEquals(domain.getPrimaryCompletionDate(), db.getPrimaryCompletionDate());
        assertEquals(domain.getCompletionDate(), db.getCompletionDate());
        assertEquals(domain.getLastUpdatePostedDate(), db.getLastUpdatePostedDate());
        assertEquals(domain.getEnrollmentCount(), db.getEnrollmentCount());
        assertEquals(domain.getEnrollmentType(), db.getEnrollmentType());
        assertEquals(domain.getHealthyVolunteers(), db.getHealthyVolunteers());
        assertEquals(domain.getSex(), db.getSex());
        assertEquals(domain.getMinimumAge(), db.getMinimumAge());
        assertEquals(domain.getMaximumAge(), db.getMaximumAge());
        assertEquals(domain.getEligibilityCriteria(), db.getEligibilityCriteria());
        assertEquals(domain.getIsPaidStudy(), db.getIsPaidStudy());
        assertEquals(domain.getPaidAmount(), db.getPaidAmount());
        assertEquals(domain.getCreatedAt(), db.getCreatedAt());
        assertEquals(domain.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(domain.getDeletedAt(), db.getDeletedAt());
        assertEquals(domain.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        // Arrange
        TrialDb db1 = DomainBuilderDatabase.getTrialDb();
        TrialDb db2 = DomainBuilderDatabase.getTrialDb();
        List<TrialDb> dbList = Arrays.asList(db1, db2);

        // Act
        List<Trial> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals(db1.getExtid(), domainList.get(0).getExtid());
        assertEquals(db2.getExtid(), domainList.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        // Arrange
        List<TrialDb> dbList = Arrays.asList();

        // Act
        List<Trial> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        // Act
        List<Trial> domainList = mapper.toModelList(null);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toDbList_shouldMapAllItems() {
        // Arrange
        Trial domain1 = DomainBuilderDatabase.getTrial();
        Trial domain2 = DomainBuilderDatabase.getTrial();
        List<Trial> domainList = Arrays.asList(domain1, domain2);

        // Act
        List<TrialDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(2, dbList.size());
        assertEquals(domain1.getExtid(), dbList.get(0).getExtid());
        assertEquals(domain2.getExtid(), dbList.get(1).getExtid());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        // Arrange
        List<Trial> domainList = Arrays.asList();

        // Act
        List<TrialDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        // Act
        List<TrialDb> dbList = mapper.toDbList(null);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }
}
