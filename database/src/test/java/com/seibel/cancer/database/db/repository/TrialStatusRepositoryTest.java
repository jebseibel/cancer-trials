package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.TrialStatusDb;
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
class TrialStatusRepositoryTest {

    @Autowired
    private TrialStatusRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnTrialStatus_whenExists() {
        // Arrange
        TrialStatusDb trialStatus = DomainBuilderDatabase.getTrialStatusDb();
        repository.save(trialStatus);

        // Act
        Optional<TrialStatusDb> result = repository.findByExtid(trialStatus.getExtid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(trialStatus.getExtid(), result.get().getExtid());
        assertEquals(trialStatus.getStatus(), result.get().getStatus());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<TrialStatusDb> result = repository.findByExtid("nonexistent-extid");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByTrialId_shouldReturnTrialStatuses_whenExists() {
        // Arrange
        TrialStatusDb trialStatus = DomainBuilderDatabase.getTrialStatusDb(500L, null);
        repository.save(trialStatus);

        // Act
        List<TrialStatusDb> result = repository.findByTrialId(500L);

        // Assert
        assertEquals(1, result.size());
        assertEquals(trialStatus.getExtid(), result.get(0).getExtid());
    }

    @Test
    void findByTrialId_shouldReturnEmpty_whenNotExists() {
        // Act
        List<TrialStatusDb> result = repository.findByTrialId(999999L);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByAppUserId_shouldReturnTrialStatuses_whenExists() {
        // Arrange
        TrialStatusDb trialStatus = DomainBuilderDatabase.getTrialStatusDb(null, 700L);
        repository.save(trialStatus);

        // Act
        List<TrialStatusDb> result = repository.findByAppUserId(700L);

        // Assert
        assertEquals(1, result.size());
        assertEquals(trialStatus.getExtid(), result.get(0).getExtid());
    }

    @Test
    void findByAppUserId_shouldReturnEmpty_whenNotExists() {
        // Act
        List<TrialStatusDb> result = repository.findByAppUserId(999999L);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        // Arrange
        TrialStatusDb active1 = DomainBuilderDatabase.getTrialStatusDb();
        active1.setActive(ActiveEnum.ACTIVE);
        TrialStatusDb active2 = DomainBuilderDatabase.getTrialStatusDb();
        active2.setActive(ActiveEnum.ACTIVE);
        TrialStatusDb inactive = DomainBuilderDatabase.getTrialStatusDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active1);
        repository.save(active2);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<TrialStatusDb> result = repository.findByActive(ActiveEnum.ACTIVE, pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(o -> o.getActive() == ActiveEnum.ACTIVE));
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        // Arrange
        TrialStatusDb active = DomainBuilderDatabase.getTrialStatusDb();
        active.setActive(ActiveEnum.ACTIVE);
        TrialStatusDb inactive = DomainBuilderDatabase.getTrialStatusDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<TrialStatusDb> result = repository.findByActive(ActiveEnum.INACTIVE, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(ActiveEnum.INACTIVE, result.getContent().get(0).getActive());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        // Arrange
        TrialStatusDb trialStatus = DomainBuilderDatabase.getTrialStatusDb();
        repository.save(trialStatus);

        // Act
        boolean result = repository.existsByExtid(trialStatus.getExtid());

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
    void save_shouldPersistTrialStatus() {
        // Arrange
        TrialStatusDb trialStatus = DomainBuilderDatabase.getTrialStatusDb();

        // Act
        TrialStatusDb saved = repository.save(trialStatus);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(trialStatus.getExtid(), saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllTrialStatuses() {
        // Arrange
        TrialStatusDb trialStatus1 = DomainBuilderDatabase.getTrialStatusDb();
        TrialStatusDb trialStatus2 = DomainBuilderDatabase.getTrialStatusDb();
        repository.save(trialStatus1);
        repository.save(trialStatus2);

        // Act
        List<TrialStatusDb> result = (List<TrialStatusDb>) repository.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void deleteById_shouldRemoveTrialStatus() {
        // Arrange
        TrialStatusDb trialStatus = DomainBuilderDatabase.getTrialStatusDb();
        TrialStatusDb saved = repository.save(trialStatus);

        // Act
        repository.deleteById(saved.getId());

        // Assert
        Optional<TrialStatusDb> result = repository.findById(saved.getId());
        assertTrue(result.isEmpty());
    }
}
