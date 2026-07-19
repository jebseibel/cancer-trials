package com.seibel.jobs.database.db.mapper;

import com.seibel.jobs.common.domain.Skill;
import com.seibel.jobs.database.db.entity.SkillDb;
import com.seibel.jobs.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SkillMapperTest {

    private SkillMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new SkillMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        // Arrange
        SkillDb db = DomainBuilderDatabase.getSkillDb();

        // Act
        Skill domain = mapper.toModel(db);

        // Assert
        assertNotNull(domain);
        assertEquals(db.getExtid(), domain.getExtid());
        assertEquals(db.getName(), domain.getName());
        assertEquals(db.getCreatedAt(), domain.getCreatedAt());
        assertEquals(db.getUpdatedAt(), domain.getUpdatedAt());
        assertEquals(db.getDeletedAt(), domain.getDeletedAt());
        assertEquals(db.getActive(), domain.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        // Arrange
        Skill domain = DomainBuilderDatabase.getSkill();

        // Act
        SkillDb db = mapper.toDb(domain);

        // Assert
        assertNotNull(db);
        assertEquals(domain.getExtid(), db.getExtid());
        assertEquals(domain.getName(), db.getName());
        assertEquals(domain.getCreatedAt(), db.getCreatedAt());
        assertEquals(domain.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(domain.getDeletedAt(), db.getDeletedAt());
        assertEquals(domain.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        // Arrange
        SkillDb db1 = DomainBuilderDatabase.getSkillDb();
        SkillDb db2 = DomainBuilderDatabase.getSkillDb();
        List<SkillDb> dbList = Arrays.asList(db1, db2);

        // Act
        List<Skill> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals(db1.getExtid(), domainList.get(0).getExtid());
        assertEquals(db2.getExtid(), domainList.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        // Arrange
        List<SkillDb> dbList = Arrays.asList();

        // Act
        List<Skill> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        // Act
        List<Skill> domainList = mapper.toModelList(null);

        // Assert
        assertNull(domainList);
    }

    @Test
    void toDbList_shouldMapAllItems() {
        // Arrange
        Skill domain1 = DomainBuilderDatabase.getSkill();
        Skill domain2 = DomainBuilderDatabase.getSkill();
        List<Skill> domainList = Arrays.asList(domain1, domain2);

        // Act
        List<SkillDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(2, dbList.size());
        assertEquals(domain1.getExtid(), dbList.get(0).getExtid());
        assertEquals(domain2.getExtid(), dbList.get(1).getExtid());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        // Arrange
        List<Skill> domainList = Arrays.asList();

        // Act
        List<SkillDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        // Act
        List<SkillDb> dbList = mapper.toDbList(null);

        // Assert
        assertNull(dbList);
    }
}
