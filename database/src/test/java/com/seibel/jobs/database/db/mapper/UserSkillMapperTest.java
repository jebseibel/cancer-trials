package com.seibel.jobs.database.db.mapper;

import com.seibel.jobs.common.domain.UserSkill;
import com.seibel.jobs.database.db.entity.UserSkillDb;
import com.seibel.jobs.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserSkillMapperTest {

    private UserSkillMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new UserSkillMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        // Arrange
        UserSkillDb db = DomainBuilderDatabase.getUserSkillDb();

        // Act
        UserSkill domain = mapper.toModel(db);

        // Assert
        assertNotNull(domain);
        assertEquals(db.getExtid(), domain.getExtid());
        assertEquals(db.getUserId(), domain.getUserId());
        assertEquals(db.getSkillId(), domain.getSkillId());
        assertEquals(db.getCreatedAt(), domain.getCreatedAt());
        assertEquals(db.getUpdatedAt(), domain.getUpdatedAt());
        assertEquals(db.getDeletedAt(), domain.getDeletedAt());
        assertEquals(db.getActive(), domain.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        // Arrange
        UserSkill domain = DomainBuilderDatabase.getUserSkill();

        // Act
        UserSkillDb db = mapper.toDb(domain);

        // Assert
        assertNotNull(db);
        assertEquals(domain.getExtid(), db.getExtid());
        assertEquals(domain.getUserId(), db.getUserId());
        assertEquals(domain.getSkillId(), db.getSkillId());
        assertEquals(domain.getCreatedAt(), db.getCreatedAt());
        assertEquals(domain.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(domain.getDeletedAt(), db.getDeletedAt());
        assertEquals(domain.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        // Arrange
        UserSkillDb db1 = DomainBuilderDatabase.getUserSkillDb();
        UserSkillDb db2 = DomainBuilderDatabase.getUserSkillDb();
        List<UserSkillDb> dbList = Arrays.asList(db1, db2);

        // Act
        List<UserSkill> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals(db1.getExtid(), domainList.get(0).getExtid());
        assertEquals(db2.getExtid(), domainList.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        // Arrange
        List<UserSkillDb> dbList = Arrays.asList();

        // Act
        List<UserSkill> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        // Act
        List<UserSkill> domainList = mapper.toModelList(null);

        // Assert
        assertNull(domainList);
    }

    @Test
    void toDbList_shouldMapAllItems() {
        // Arrange
        UserSkill domain1 = DomainBuilderDatabase.getUserSkill();
        UserSkill domain2 = DomainBuilderDatabase.getUserSkill();
        List<UserSkill> domainList = Arrays.asList(domain1, domain2);

        // Act
        List<UserSkillDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(2, dbList.size());
        assertEquals(domain1.getExtid(), dbList.get(0).getExtid());
        assertEquals(domain2.getExtid(), dbList.get(1).getExtid());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        // Arrange
        List<UserSkill> domainList = Arrays.asList();

        // Act
        List<UserSkillDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        // Act
        List<UserSkillDb> dbList = mapper.toDbList(null);

        // Assert
        assertNull(dbList);
    }
}
