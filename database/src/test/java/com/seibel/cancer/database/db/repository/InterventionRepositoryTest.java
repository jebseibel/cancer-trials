package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.InterventionDb;
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
class InterventionRepositoryTest {

    @Autowired
    private InterventionRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnIntervention_whenExists() {
        // Arrange
        InterventionDb intervention = DomainBuilderDatabase.getInterventionDb();
        repository.save(intervention);

        // Act
        Optional<InterventionDb> result = repository.findByExtid(intervention.getExtid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(intervention.getExtid(), result.get().getExtid());
        assertEquals(intervention.getName(), result.get().getName());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<InterventionDb> result = repository.findByExtid("nonexistent-extid");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByTrialId_shouldReturnInterventions_whenExists() {
        // Arrange
        InterventionDb intervention1 = DomainBuilderDatabase.getInterventionDb(null, null, 555L, null);
        InterventionDb intervention2 = DomainBuilderDatabase.getInterventionDb(null, null, 555L, null);
        InterventionDb otherTrial = DomainBuilderDatabase.getInterventionDb(null, null, 999L, null);

        repository.save(intervention1);
        repository.save(intervention2);
        repository.save(otherTrial);

        // Act
        List<InterventionDb> result = repository.findByTrialId(555L);

        // Assert
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(o -> o.getTrialId().equals(555L)));
    }

    @Test
    void findByTrialId_shouldReturnEmpty_whenNotExists() {
        // Act
        List<InterventionDb> result = repository.findByTrialId(123456L);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        // Arrange
        InterventionDb active1 = DomainBuilderDatabase.getInterventionDb();
        active1.setActive(ActiveEnum.ACTIVE);
        InterventionDb active2 = DomainBuilderDatabase.getInterventionDb();
        active2.setActive(ActiveEnum.ACTIVE);
        InterventionDb inactive = DomainBuilderDatabase.getInterventionDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active1);
        repository.save(active2);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<InterventionDb> result = repository.findByActive(ActiveEnum.ACTIVE, pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(o -> o.getActive() == ActiveEnum.ACTIVE));
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        // Arrange
        InterventionDb active = DomainBuilderDatabase.getInterventionDb();
        active.setActive(ActiveEnum.ACTIVE);
        InterventionDb inactive = DomainBuilderDatabase.getInterventionDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<InterventionDb> result = repository.findByActive(ActiveEnum.INACTIVE, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(ActiveEnum.INACTIVE, result.getContent().get(0).getActive());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        // Arrange
        InterventionDb intervention = DomainBuilderDatabase.getInterventionDb();
        repository.save(intervention);

        // Act
        boolean result = repository.existsByExtid(intervention.getExtid());

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
    void save_shouldPersistIntervention() {
        // Arrange
        InterventionDb intervention = DomainBuilderDatabase.getInterventionDb();

        // Act
        InterventionDb saved = repository.save(intervention);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(intervention.getExtid(), saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllInterventions() {
        // Arrange
        InterventionDb intervention1 = DomainBuilderDatabase.getInterventionDb();
        InterventionDb intervention2 = DomainBuilderDatabase.getInterventionDb();
        repository.save(intervention1);
        repository.save(intervention2);

        // Act
        List<InterventionDb> result = (List<InterventionDb>) repository.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void deleteById_shouldRemoveIntervention() {
        // Arrange
        InterventionDb intervention = DomainBuilderDatabase.getInterventionDb();
        InterventionDb saved = repository.save(intervention);

        // Act
        repository.deleteById(saved.getId());

        // Assert
        Optional<InterventionDb> result = repository.findById(saved.getId());
        assertTrue(result.isEmpty());
    }
}
