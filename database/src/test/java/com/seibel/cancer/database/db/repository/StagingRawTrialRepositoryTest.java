package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.StagingRawTrialDb;
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
class StagingRawTrialRepositoryTest {

    @Autowired
    private StagingRawTrialRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnStagingRawTrial_whenExists() {
        // Arrange
        StagingRawTrialDb stagingRawTrial = DomainBuilderDatabase.getStagingRawTrialDb();
        repository.save(stagingRawTrial);

        // Act
        Optional<StagingRawTrialDb> result = repository.findByExtid(stagingRawTrial.getExtid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(stagingRawTrial.getExtid(), result.get().getExtid());
        assertEquals(stagingRawTrial.getSourceTrialId(), result.get().getSourceTrialId());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<StagingRawTrialDb> result = repository.findByExtid("nonexistent-extid");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findBySourceTrialId_shouldReturnStagingRawTrial_whenExists() {
        // Arrange
        StagingRawTrialDb stagingRawTrial = DomainBuilderDatabase.getStagingRawTrialDb(null, "SRC_12345678");
        repository.save(stagingRawTrial);

        // Act
        List<StagingRawTrialDb> result = repository.findBySourceTrialId("SRC_12345678");

        // Assert
        assertEquals(1, result.size());
        assertEquals(stagingRawTrial.getExtid(), result.get(0).getExtid());
    }

    @Test
    void findBySourceTrialId_shouldReturnEmpty_whenNotExists() {
        // Act
        List<StagingRawTrialDb> result = repository.findBySourceTrialId("SRC_00000000");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        // Arrange
        StagingRawTrialDb active1 = DomainBuilderDatabase.getStagingRawTrialDb();
        active1.setActive(ActiveEnum.ACTIVE);
        StagingRawTrialDb active2 = DomainBuilderDatabase.getStagingRawTrialDb();
        active2.setActive(ActiveEnum.ACTIVE);
        StagingRawTrialDb inactive = DomainBuilderDatabase.getStagingRawTrialDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active1);
        repository.save(active2);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<StagingRawTrialDb> result = repository.findByActive(ActiveEnum.ACTIVE, pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(o -> o.getActive() == ActiveEnum.ACTIVE));
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        // Arrange
        StagingRawTrialDb active = DomainBuilderDatabase.getStagingRawTrialDb();
        active.setActive(ActiveEnum.ACTIVE);
        StagingRawTrialDb inactive = DomainBuilderDatabase.getStagingRawTrialDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<StagingRawTrialDb> result = repository.findByActive(ActiveEnum.INACTIVE, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(ActiveEnum.INACTIVE, result.getContent().get(0).getActive());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        // Arrange
        StagingRawTrialDb stagingRawTrial = DomainBuilderDatabase.getStagingRawTrialDb();
        repository.save(stagingRawTrial);

        // Act
        boolean result = repository.existsByExtid(stagingRawTrial.getExtid());

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
    void save_shouldPersistStagingRawTrial() {
        // Arrange
        StagingRawTrialDb stagingRawTrial = DomainBuilderDatabase.getStagingRawTrialDb();

        // Act
        StagingRawTrialDb saved = repository.save(stagingRawTrial);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(stagingRawTrial.getExtid(), saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllStagingRawTrials() {
        // Arrange
        StagingRawTrialDb stagingRawTrial1 = DomainBuilderDatabase.getStagingRawTrialDb();
        StagingRawTrialDb stagingRawTrial2 = DomainBuilderDatabase.getStagingRawTrialDb();
        repository.save(stagingRawTrial1);
        repository.save(stagingRawTrial2);

        // Act
        List<StagingRawTrialDb> result = (List<StagingRawTrialDb>) repository.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void deleteById_shouldRemoveStagingRawTrial() {
        // Arrange
        StagingRawTrialDb stagingRawTrial = DomainBuilderDatabase.getStagingRawTrialDb();
        StagingRawTrialDb saved = repository.save(stagingRawTrial);

        // Act
        repository.deleteById(saved.getId());

        // Assert
        Optional<StagingRawTrialDb> result = repository.findById(saved.getId());
        assertTrue(result.isEmpty());
    }
}
