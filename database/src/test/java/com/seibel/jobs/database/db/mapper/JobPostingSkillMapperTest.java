package com.seibel.jobs.database.db.mapper;

import com.seibel.jobs.common.domain.JobPostingSkill;
import com.seibel.jobs.database.db.entity.JobPostingSkillDb;
import com.seibel.jobs.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class JobPostingSkillMapperTest {

    private JobPostingSkillMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new JobPostingSkillMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        // Arrange
        JobPostingSkillDb db = DomainBuilderDatabase.getJobPostingSkillDb();

        // Act
        JobPostingSkill domain = mapper.toModel(db);

        // Assert
        assertNotNull(domain);
        assertEquals(db.getExtid(), domain.getExtid());
        assertEquals(db.getJobPostingId(), domain.getJobPostingId());
        assertEquals(db.getSkillId(), domain.getSkillId());
        assertEquals(db.getCreatedAt(), domain.getCreatedAt());
        assertEquals(db.getUpdatedAt(), domain.getUpdatedAt());
        assertEquals(db.getDeletedAt(), domain.getDeletedAt());
        assertEquals(db.getActive(), domain.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        // Arrange
        JobPostingSkill domain = DomainBuilderDatabase.getJobPostingSkill();

        // Act
        JobPostingSkillDb db = mapper.toDb(domain);

        // Assert
        assertNotNull(db);
        assertEquals(domain.getExtid(), db.getExtid());
        assertEquals(domain.getJobPostingId(), db.getJobPostingId());
        assertEquals(domain.getSkillId(), db.getSkillId());
        assertEquals(domain.getCreatedAt(), db.getCreatedAt());
        assertEquals(domain.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(domain.getDeletedAt(), db.getDeletedAt());
        assertEquals(domain.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        // Arrange
        JobPostingSkillDb db1 = DomainBuilderDatabase.getJobPostingSkillDb();
        JobPostingSkillDb db2 = DomainBuilderDatabase.getJobPostingSkillDb();
        List<JobPostingSkillDb> dbList = Arrays.asList(db1, db2);

        // Act
        List<JobPostingSkill> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals(db1.getExtid(), domainList.get(0).getExtid());
        assertEquals(db2.getExtid(), domainList.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        // Arrange
        List<JobPostingSkillDb> dbList = Arrays.asList();

        // Act
        List<JobPostingSkill> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        // Act
        List<JobPostingSkill> domainList = mapper.toModelList(null);

        // Assert
        assertNull(domainList);
    }

    @Test
    void toDbList_shouldMapAllItems() {
        // Arrange
        JobPostingSkill domain1 = DomainBuilderDatabase.getJobPostingSkill();
        JobPostingSkill domain2 = DomainBuilderDatabase.getJobPostingSkill();
        List<JobPostingSkill> domainList = Arrays.asList(domain1, domain2);

        // Act
        List<JobPostingSkillDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(2, dbList.size());
        assertEquals(domain1.getExtid(), dbList.get(0).getExtid());
        assertEquals(domain2.getExtid(), dbList.get(1).getExtid());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        // Arrange
        List<JobPostingSkill> domainList = Arrays.asList();

        // Act
        List<JobPostingSkillDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        // Act
        List<JobPostingSkillDb> dbList = mapper.toDbList(null);

        // Assert
        assertNull(dbList);
    }
}
