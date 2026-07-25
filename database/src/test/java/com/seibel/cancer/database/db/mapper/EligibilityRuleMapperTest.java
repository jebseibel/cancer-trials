package com.seibel.cancer.database.db.mapper;

import com.seibel.cancer.common.domain.EligibilityRule;
import com.seibel.cancer.database.db.entity.EligibilityRuleDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EligibilityRuleMapperTest {

    private EligibilityRuleMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new EligibilityRuleMapper();
    }

    @Test
    void toModel_shouldMapAllFields() {
        // Arrange
        EligibilityRuleDb db = DomainBuilderDatabase.getEligibilityRuleDb();

        // Act
        EligibilityRule domain = mapper.toModel(db);

        // Assert
        assertNotNull(domain);
        assertEquals(db.getExtid(), domain.getExtid());
        assertEquals(db.getTrialId(), domain.getTrialId());
        assertEquals(db.getParentRuleId(), domain.getParentRuleId());
        assertEquals(db.getNodeType(), domain.getNodeType());
        assertEquals(db.getOperator(), domain.getOperator());
        assertEquals(db.getCriterionType(), domain.getCriterionType());
        assertEquals(db.getCriterionId(), domain.getCriterionId());
        assertEquals(db.getRequirementType(), domain.getRequirementType());
        assertEquals(db.getSortOrder(), domain.getSortOrder());
        assertEquals(db.getNotes(), domain.getNotes());
        assertEquals(db.getCreatedAt(), domain.getCreatedAt());
        assertEquals(db.getUpdatedAt(), domain.getUpdatedAt());
        assertEquals(db.getDeletedAt(), domain.getDeletedAt());
        assertEquals(db.getActive(), domain.getActive());
    }

    @Test
    void toDb_shouldMapAllFields() {
        // Arrange
        EligibilityRule domain = DomainBuilderDatabase.getEligibilityRule();

        // Act
        EligibilityRuleDb db = mapper.toDb(domain);

        // Assert
        assertNotNull(db);
        assertEquals(domain.getExtid(), db.getExtid());
        assertEquals(domain.getTrialId(), db.getTrialId());
        assertEquals(domain.getParentRuleId(), db.getParentRuleId());
        assertEquals(domain.getNodeType(), db.getNodeType());
        assertEquals(domain.getOperator(), db.getOperator());
        assertEquals(domain.getCriterionType(), db.getCriterionType());
        assertEquals(domain.getCriterionId(), db.getCriterionId());
        assertEquals(domain.getRequirementType(), db.getRequirementType());
        assertEquals(domain.getSortOrder(), db.getSortOrder());
        assertEquals(domain.getNotes(), db.getNotes());
        assertEquals(domain.getCreatedAt(), db.getCreatedAt());
        assertEquals(domain.getUpdatedAt(), db.getUpdatedAt());
        assertEquals(domain.getDeletedAt(), db.getDeletedAt());
        assertEquals(domain.getActive(), db.getActive());
    }

    @Test
    void toModelList_shouldMapAllItems() {
        // Arrange
        EligibilityRuleDb db1 = DomainBuilderDatabase.getEligibilityRuleDb();
        EligibilityRuleDb db2 = DomainBuilderDatabase.getEligibilityRuleDb();
        List<EligibilityRuleDb> dbList = Arrays.asList(db1, db2);

        // Act
        List<EligibilityRule> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(2, domainList.size());
        assertEquals(db1.getExtid(), domainList.get(0).getExtid());
        assertEquals(db2.getExtid(), domainList.get(1).getExtid());
    }

    @Test
    void toModelList_shouldHandleEmptyList() {
        // Arrange
        List<EligibilityRuleDb> dbList = Arrays.asList();

        // Act
        List<EligibilityRule> domainList = mapper.toModelList(dbList);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toModelList_shouldHandleNullList() {
        // Act
        List<EligibilityRule> domainList = mapper.toModelList(null);

        // Assert
        assertNotNull(domainList);
        assertEquals(0, domainList.size());
    }

    @Test
    void toDbList_shouldMapAllItems() {
        // Arrange
        EligibilityRule domain1 = DomainBuilderDatabase.getEligibilityRule();
        EligibilityRule domain2 = DomainBuilderDatabase.getEligibilityRule();
        List<EligibilityRule> domainList = Arrays.asList(domain1, domain2);

        // Act
        List<EligibilityRuleDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(2, dbList.size());
        assertEquals(domain1.getExtid(), dbList.get(0).getExtid());
        assertEquals(domain2.getExtid(), dbList.get(1).getExtid());
    }

    @Test
    void toDbList_shouldHandleEmptyList() {
        // Arrange
        List<EligibilityRule> domainList = Arrays.asList();

        // Act
        List<EligibilityRuleDb> dbList = mapper.toDbList(domainList);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }

    @Test
    void toDbList_shouldHandleNullList() {
        // Act
        List<EligibilityRuleDb> dbList = mapper.toDbList(null);

        // Assert
        assertNotNull(dbList);
        assertEquals(0, dbList.size());
    }
}
