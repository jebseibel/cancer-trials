package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.OverallOfficialDb;
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
class OverallOfficialRepositoryTest {

    @Autowired
    private OverallOfficialRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnOverallOfficial_whenExists() {
        // Arrange
        OverallOfficialDb overallOfficial = DomainBuilderDatabase.getOverallOfficialDb();
        repository.save(overallOfficial);

        // Act
        Optional<OverallOfficialDb> result = repository.findByExtid(overallOfficial.getExtid());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(overallOfficial.getExtid(), result.get().getExtid());
        assertEquals(overallOfficial.getName(), result.get().getName());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        // Act
        Optional<OverallOfficialDb> result = repository.findByExtid("nonexistent-extid");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByTrialId_shouldReturnOverallOfficials_whenExists() {
        // Arrange
        OverallOfficialDb overallOfficial = DomainBuilderDatabase.getOverallOfficialDb(42L, null);
        repository.save(overallOfficial);

        // Act
        List<OverallOfficialDb> result = repository.findByTrialId(42L);

        // Assert
        assertEquals(1, result.size());
        assertEquals(overallOfficial.getExtid(), result.get(0).getExtid());
    }

    @Test
    void findByTrialId_shouldReturnEmpty_whenNotExists() {
        // Act
        List<OverallOfficialDb> result = repository.findByTrialId(999999L);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findByActive_shouldReturnActiveOnly() {
        // Arrange
        OverallOfficialDb active1 = DomainBuilderDatabase.getOverallOfficialDb();
        active1.setActive(ActiveEnum.ACTIVE);
        OverallOfficialDb active2 = DomainBuilderDatabase.getOverallOfficialDb();
        active2.setActive(ActiveEnum.ACTIVE);
        OverallOfficialDb inactive = DomainBuilderDatabase.getOverallOfficialDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active1);
        repository.save(active2);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<OverallOfficialDb> result = repository.findByActive(ActiveEnum.ACTIVE, pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertTrue(result.getContent().stream().allMatch(o -> o.getActive() == ActiveEnum.ACTIVE));
    }

    @Test
    void findByActive_shouldReturnInactiveOnly() {
        // Arrange
        OverallOfficialDb active = DomainBuilderDatabase.getOverallOfficialDb();
        active.setActive(ActiveEnum.ACTIVE);
        OverallOfficialDb inactive = DomainBuilderDatabase.getOverallOfficialDb();
        inactive.setActive(ActiveEnum.INACTIVE);

        repository.save(active);
        repository.save(inactive);

        // Act
        Pageable pageable = PageRequest.of(0, 10);
        Page<OverallOfficialDb> result = repository.findByActive(ActiveEnum.INACTIVE, pageable);

        // Assert
        assertEquals(1, result.getContent().size());
        assertEquals(ActiveEnum.INACTIVE, result.getContent().get(0).getActive());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        // Arrange
        OverallOfficialDb overallOfficial = DomainBuilderDatabase.getOverallOfficialDb();
        repository.save(overallOfficial);

        // Act
        boolean result = repository.existsByExtid(overallOfficial.getExtid());

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
    void save_shouldPersistOverallOfficial() {
        // Arrange
        OverallOfficialDb overallOfficial = DomainBuilderDatabase.getOverallOfficialDb();

        // Act
        OverallOfficialDb saved = repository.save(overallOfficial);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(overallOfficial.getExtid(), saved.getExtid());
    }

    @Test
    void findAll_shouldReturnAllOverallOfficials() {
        // Arrange
        OverallOfficialDb overallOfficial1 = DomainBuilderDatabase.getOverallOfficialDb();
        OverallOfficialDb overallOfficial2 = DomainBuilderDatabase.getOverallOfficialDb();
        repository.save(overallOfficial1);
        repository.save(overallOfficial2);

        // Act
        List<OverallOfficialDb> result = (List<OverallOfficialDb>) repository.findAll();

        // Assert
        assertEquals(2, result.size());
    }

    @Test
    void deleteById_shouldRemoveOverallOfficial() {
        // Arrange
        OverallOfficialDb overallOfficial = DomainBuilderDatabase.getOverallOfficialDb();
        OverallOfficialDb saved = repository.save(overallOfficial);

        // Act
        repository.deleteById(saved.getId());

        // Assert
        Optional<OverallOfficialDb> result = repository.findById(saved.getId());
        assertTrue(result.isEmpty());
    }
}
