package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.ArmGroupDb;
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
class ArmGroupRepositoryTest {

    @Autowired
    private ArmGroupRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnArmGroup_whenExists() {
        // Arrange
        ArmGroupDb armGroup = DomainBuilderDatabase.getArmGroupDb();
        repository.save(armGroup);

        // Act
        Optional<ArmGroupDb> result = repository.findByExtid(armGroup.getExtid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(armGroup.getExtid(), result.get().getExtid());
        assertEquals(armGroup.getLabel(), result.get().getLabel());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<ArmGroupDb> result = repository.findByExtid("nonexistent-extid");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByTrialId_shouldReturnArmGroups_whenExists() {
        // Arrange
        Long trialId = 42L;
        ArmGroupDb armGroup1 = DomainBuilderDatabase.getArmGroupDb(trialId, null);
        ArmGroupDb armGroup2 = DomainBuilderDatabase.getArmGroupDb(trialId, null);
        ArmGroupDb otherTrial = DomainBuilderDatabase.getArmGroupDb(99L, null);

        repository.save(armGroup1);
        repository.save(armGroup2);
        repository.save(otherTrial);

        // Act
        List<ArmGroupDb> result = repository.findByTrialId(trialId);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(o -> o.getTrialId().equals(trialId)));
    }

    @Test
    void findByTrialId_shouldReturnEmpty_whenNotExists() {
        // Act
        List<ArmGroupDb> result = repository.findByTrialId(12345L);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        // Arrange
        ArmGroupDb active1 = DomainBuilderDatabase.getArmGroupDb();
        active1.setActive(ActiveEnum.ACTIVE);
        ArmGroupDb active2 = DomainBuilderDatabase.getArmGroupDb();
        active2.setActive(ActiveEnum.ACTIVE);
        ArmGroupDb inactive = DomainBuilderDatabase.getArmGroupDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active1);
        repository.save(active2);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<ArmGroupDb> result = repository.findByActive(ActiveEnum.ACTIVE, pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(o -> o.getActive() == ActiveEnum.ACTIVE));
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        // Arrange
        ArmGroupDb active = DomainBuilderDatabase.getArmGroupDb();
        active.setActive(ActiveEnum.ACTIVE);
        ArmGroupDb inactive = DomainBuilderDatabase.getArmGroupDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<ArmGroupDb> result = repository.findByActive(ActiveEnum.INACTIVE, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(ActiveEnum.INACTIVE, result.getContent().get(0).getActive());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        // Arrange
        ArmGroupDb armGroup = DomainBuilderDatabase.getArmGroupDb();
        repository.save(armGroup);

        // Act
        boolean result = repository.existsByExtid(armGroup.getExtid());

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
    void save_shouldPersistArmGroup() {
        // Arrange
        ArmGroupDb armGroup = DomainBuilderDatabase.getArmGroupDb();

        // Act
        ArmGroupDb saved = repository.save(armGroup);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(armGroup.getExtid(), saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllArmGroups() {
        // Arrange
        ArmGroupDb armGroup1 = DomainBuilderDatabase.getArmGroupDb();
        ArmGroupDb armGroup2 = DomainBuilderDatabase.getArmGroupDb();
        repository.save(armGroup1);
        repository.save(armGroup2);

        // Act
        List<ArmGroupDb> result = (List<ArmGroupDb>) repository.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void deleteById_shouldRemoveArmGroup() {
        // Arrange
        ArmGroupDb armGroup = DomainBuilderDatabase.getArmGroupDb();
        ArmGroupDb saved = repository.save(armGroup);

        // Act
        repository.deleteById(saved.getId());

        // Assert
        Optional<ArmGroupDb> result = repository.findById(saved.getId());
        assertTrue(result.isEmpty());
    }
}
