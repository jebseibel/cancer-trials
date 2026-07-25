package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.TrialDb;
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
class TrialRepositoryTest {

    @Autowired
    private TrialRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnTrial_whenExists() {
        // Arrange
        TrialDb trial = DomainBuilderDatabase.getTrialDb();
        repository.save(trial);

        // Act
        Optional<TrialDb> result = repository.findByExtid(trial.getExtid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(trial.getExtid(), result.get().getExtid());
        assertEquals(trial.getBriefTitle(), result.get().getBriefTitle());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<TrialDb> result = repository.findByExtid("nonexistent-extid");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByNctId_shouldReturnTrial_whenExists() {
        // Arrange
        TrialDb trial = DomainBuilderDatabase.getTrialDb("NCT12345678", null);
        repository.save(trial);

        // Act
        Optional<TrialDb> result = repository.findByNctId("NCT12345678");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(trial.getExtid(), result.get().getExtid());
    }

    @Test
    void findByNctId_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<TrialDb> result = repository.findByNctId("NCT00000000");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        // Arrange
        TrialDb active1 = DomainBuilderDatabase.getTrialDb();
        active1.setActive(ActiveEnum.ACTIVE);
        TrialDb active2 = DomainBuilderDatabase.getTrialDb();
        active2.setActive(ActiveEnum.ACTIVE);
        TrialDb inactive = DomainBuilderDatabase.getTrialDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active1);
        repository.save(active2);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<TrialDb> result = repository.findByActive(ActiveEnum.ACTIVE, pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(o -> o.getActive() == ActiveEnum.ACTIVE));
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        // Arrange
        TrialDb active = DomainBuilderDatabase.getTrialDb();
        active.setActive(ActiveEnum.ACTIVE);
        TrialDb inactive = DomainBuilderDatabase.getTrialDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<TrialDb> result = repository.findByActive(ActiveEnum.INACTIVE, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(ActiveEnum.INACTIVE, result.getContent().get(0).getActive());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        // Arrange
        TrialDb trial = DomainBuilderDatabase.getTrialDb();
        repository.save(trial);

        // Act
        boolean result = repository.existsByExtid(trial.getExtid());

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
    void save_shouldPersistTrial() {
        // Arrange
        TrialDb trial = DomainBuilderDatabase.getTrialDb();

        // Act
        TrialDb saved = repository.save(trial);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(trial.getExtid(), saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllTrials() {
        // Arrange
        TrialDb trial1 = DomainBuilderDatabase.getTrialDb();
        TrialDb trial2 = DomainBuilderDatabase.getTrialDb();
        repository.save(trial1);
        repository.save(trial2);

        // Act
        List<TrialDb> result = (List<TrialDb>) repository.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void deleteById_shouldRemoveTrial() {
        // Arrange
        TrialDb trial = DomainBuilderDatabase.getTrialDb();
        TrialDb saved = repository.save(trial);

        // Act
        repository.deleteById(saved.getId());

        // Assert
        Optional<TrialDb> result = repository.findById(saved.getId());
        assertTrue(result.isEmpty());
    }
}
