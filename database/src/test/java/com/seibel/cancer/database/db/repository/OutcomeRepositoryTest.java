package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.OutcomeDb;
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
class OutcomeRepositoryTest {

    @Autowired
    private OutcomeRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnOutcome_whenExists() {
        // Arrange
        OutcomeDb outcome = DomainBuilderDatabase.getOutcomeDb();
        repository.save(outcome);

        // Act
        Optional<OutcomeDb> result = repository.findByExtid(outcome.getExtid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(outcome.getExtid(), result.get().getExtid());
        assertEquals(outcome.getMeasure(), result.get().getMeasure());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<OutcomeDb> result = repository.findByExtid("nonexistent-extid");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        // Arrange
        OutcomeDb active1 = DomainBuilderDatabase.getOutcomeDb();
        active1.setActive(ActiveEnum.ACTIVE);
        OutcomeDb active2 = DomainBuilderDatabase.getOutcomeDb();
        active2.setActive(ActiveEnum.ACTIVE);
        OutcomeDb inactive = DomainBuilderDatabase.getOutcomeDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active1);
        repository.save(active2);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<OutcomeDb> result = repository.findByActive(ActiveEnum.ACTIVE, pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(o -> o.getActive() == ActiveEnum.ACTIVE));
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        // Arrange
        OutcomeDb active = DomainBuilderDatabase.getOutcomeDb();
        active.setActive(ActiveEnum.ACTIVE);
        OutcomeDb inactive = DomainBuilderDatabase.getOutcomeDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<OutcomeDb> result = repository.findByActive(ActiveEnum.INACTIVE, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(ActiveEnum.INACTIVE, result.getContent().get(0).getActive());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        // Arrange
        OutcomeDb outcome = DomainBuilderDatabase.getOutcomeDb();
        repository.save(outcome);

        // Act
        boolean result = repository.existsByExtid(outcome.getExtid());

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
    void save_shouldPersistOutcome() {
        // Arrange
        OutcomeDb outcome = DomainBuilderDatabase.getOutcomeDb();

        // Act
        OutcomeDb saved = repository.save(outcome);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(outcome.getExtid(), saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllOutcomes() {
        // Arrange
        OutcomeDb outcome1 = DomainBuilderDatabase.getOutcomeDb();
        OutcomeDb outcome2 = DomainBuilderDatabase.getOutcomeDb();
        repository.save(outcome1);
        repository.save(outcome2);

        // Act
        List<OutcomeDb> result = (List<OutcomeDb>) repository.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void deleteById_shouldRemoveOutcome() {
        // Arrange
        OutcomeDb outcome = DomainBuilderDatabase.getOutcomeDb();
        OutcomeDb saved = repository.save(outcome);

        // Act
        repository.deleteById(saved.getId());

        // Assert
        Optional<OutcomeDb> result = repository.findById(saved.getId());
        assertTrue(result.isEmpty());
    }
}
