package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.EligibilityRuleDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test-database")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EligibilityRuleRepositoryTest {

    @Autowired
    private EligibilityRuleRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnEligibilityRule_whenExists() {
        // Arrange
        EligibilityRuleDb rule = DomainBuilderDatabase.getEligibilityRuleDb();
        repository.save(rule);

        // Act
        Optional<EligibilityRuleDb> result = repository.findByExtid(rule.getExtid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(rule.getExtid(), result.get().getExtid());
        assertEquals(rule.getNodeType(), result.get().getNodeType());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<EligibilityRuleDb> result = repository.findByExtid("nonexistent-extid");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByTrialId_shouldReturnRules_whenExists() {
        // Arrange
        Long trialId = 555L;
        EligibilityRuleDb rule1 = DomainBuilderDatabase.getEligibilityRuleDb(trialId, null);
        EligibilityRuleDb rule2 = DomainBuilderDatabase.getEligibilityRuleDb(trialId, null);
        EligibilityRuleDb otherTrialRule = DomainBuilderDatabase.getEligibilityRuleDb(999L, null);

        repository.save(rule1);
        repository.save(rule2);
        repository.save(otherTrialRule);

        // Act
        List<EligibilityRuleDb> result = repository.findByTrialId(trialId);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(r -> r.getTrialId().equals(trialId)));
    }

    @Test
    void findByTrialId_shouldReturnEmpty_whenNotExists() {
        // Act
        List<EligibilityRuleDb> result = repository.findByTrialId(123456L);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        // Arrange
        EligibilityRuleDb active1 = DomainBuilderDatabase.getEligibilityRuleDb();
        active1.setActive(ActiveEnum.ACTIVE);
        EligibilityRuleDb active2 = DomainBuilderDatabase.getEligibilityRuleDb();
        active2.setActive(ActiveEnum.ACTIVE);
        EligibilityRuleDb inactive = DomainBuilderDatabase.getEligibilityRuleDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active1);
        repository.save(active2);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<EligibilityRuleDb> result = repository.findByActive(ActiveEnum.ACTIVE, pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(o -> o.getActive() == ActiveEnum.ACTIVE));
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        // Arrange
        EligibilityRuleDb active = DomainBuilderDatabase.getEligibilityRuleDb();
        active.setActive(ActiveEnum.ACTIVE);
        EligibilityRuleDb inactive = DomainBuilderDatabase.getEligibilityRuleDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<EligibilityRuleDb> result = repository.findByActive(ActiveEnum.INACTIVE, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(ActiveEnum.INACTIVE, result.getContent().get(0).getActive());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        // Arrange
        EligibilityRuleDb rule = DomainBuilderDatabase.getEligibilityRuleDb();
        repository.save(rule);

        // Act
        boolean result = repository.existsByExtid(rule.getExtid());

        // Assert
        assertTrue(result);
    }

    @Test
    void existsByExtid_shouldReturnFalse_whenNotExists() {
        // Act
        boolean result = repository.existsByExtid("nonexistent-extid");

        // Assert
        assertFalse(result);
    }

    @Test
    void save_shouldPersistEligibilityRule() {
        // Arrange
        EligibilityRuleDb rule = DomainBuilderDatabase.getEligibilityRuleDb();

        // Act
        EligibilityRuleDb saved = repository.save(rule);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(rule.getExtid(), saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllEligibilityRules() {
        // Arrange
        EligibilityRuleDb rule1 = DomainBuilderDatabase.getEligibilityRuleDb();
        EligibilityRuleDb rule2 = DomainBuilderDatabase.getEligibilityRuleDb();
        repository.save(rule1);
        repository.save(rule2);

        // Act
        List<EligibilityRuleDb> result = (List<EligibilityRuleDb>) repository.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void deleteById_shouldRemoveEligibilityRule() {
        // Arrange
        EligibilityRuleDb rule = DomainBuilderDatabase.getEligibilityRuleDb();
        EligibilityRuleDb saved = repository.save(rule);

        // Act
        repository.deleteById(saved.getId());

        // Assert
        Optional<EligibilityRuleDb> result = repository.findById(saved.getId());
        assertTrue(result.isEmpty());
    }
}
