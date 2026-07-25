package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.TrialSourceDb;
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
class TrialSourceRepositoryTest {

    @Autowired
    private TrialSourceRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnTrialSource_whenExists() {
        // Arrange
        TrialSourceDb trialSource = DomainBuilderDatabase.getTrialSourceDb();
        repository.save(trialSource);

        // Act
        Optional<TrialSourceDb> result = repository.findByExtid(trialSource.getExtid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(trialSource.getExtid(), result.get().getExtid());
        assertEquals(trialSource.getName(), result.get().getName());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<TrialSourceDb> result = repository.findByExtid("nonexistent-extid");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByCode_shouldReturnTrialSource_whenExists() {
        // Arrange
        TrialSourceDb trialSource = DomainBuilderDatabase.getTrialSourceDb("CTGOV", null);
        repository.save(trialSource);

        // Act
        Optional<TrialSourceDb> result = repository.findByCode("CTGOV");

        // Assert
        assertTrue(result.isPresent());
        assertEquals(trialSource.getExtid(), result.get().getExtid());
    }

    @Test
    void findByCode_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<TrialSourceDb> result = repository.findByCode("NONEXISTENT");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        // Arrange
        TrialSourceDb active1 = DomainBuilderDatabase.getTrialSourceDb();
        active1.setActive(ActiveEnum.ACTIVE);
        TrialSourceDb active2 = DomainBuilderDatabase.getTrialSourceDb();
        active2.setActive(ActiveEnum.ACTIVE);
        TrialSourceDb inactive = DomainBuilderDatabase.getTrialSourceDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active1);
        repository.save(active2);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<TrialSourceDb> result = repository.findByActive(ActiveEnum.ACTIVE, pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(o -> o.getActive() == ActiveEnum.ACTIVE));
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        // Arrange
        TrialSourceDb active = DomainBuilderDatabase.getTrialSourceDb();
        active.setActive(ActiveEnum.ACTIVE);
        TrialSourceDb inactive = DomainBuilderDatabase.getTrialSourceDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<TrialSourceDb> result = repository.findByActive(ActiveEnum.INACTIVE, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(ActiveEnum.INACTIVE, result.getContent().get(0).getActive());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        // Arrange
        TrialSourceDb trialSource = DomainBuilderDatabase.getTrialSourceDb();
        repository.save(trialSource);

        // Act
        boolean result = repository.existsByExtid(trialSource.getExtid());

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
    void save_shouldPersistTrialSource() {
        // Arrange
        TrialSourceDb trialSource = DomainBuilderDatabase.getTrialSourceDb();

        // Act
        TrialSourceDb saved = repository.save(trialSource);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(trialSource.getExtid(), saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllTrialSources() {
        // Arrange
        TrialSourceDb trialSource1 = DomainBuilderDatabase.getTrialSourceDb();
        TrialSourceDb trialSource2 = DomainBuilderDatabase.getTrialSourceDb();
        repository.save(trialSource1);
        repository.save(trialSource2);

        // Act
        List<TrialSourceDb> result = (List<TrialSourceDb>) repository.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void deleteById_shouldRemoveTrialSource() {
        // Arrange
        TrialSourceDb trialSource = DomainBuilderDatabase.getTrialSourceDb();
        TrialSourceDb saved = repository.save(trialSource);

        // Act
        repository.deleteById(saved.getId());

        // Assert
        Optional<TrialSourceDb> result = repository.findById(saved.getId());
        assertTrue(result.isEmpty());
    }
}
