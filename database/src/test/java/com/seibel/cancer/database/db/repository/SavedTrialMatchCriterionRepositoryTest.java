package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.SavedTrialMatchCriterionDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test-database")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SavedTrialMatchCriterionRepositoryTest {

    @Autowired
    private SavedTrialMatchCriterionRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnCriterion_whenExists() {
        SavedTrialMatchCriterionDb saved = repository.save(DomainBuilderDatabase.getSavedTrialMatchCriterionDb());

        Optional<SavedTrialMatchCriterionDb> found = repository.findByExtid(saved.getExtid());

        assertTrue(found.isPresent());
        assertEquals(saved.getExtid(), found.get().getExtid());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        assertTrue(repository.findByExtid(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    void findByTrialMatchId_shouldReturnCriteriaInScoreOrder() {
        Long matchId = 555L;

        SavedTrialMatchCriterionDb low = DomainBuilderDatabase.getSavedTrialMatchCriterionDb(matchId, null);
        low.setScore(new BigDecimal("0.4000"));
        repository.save(low);

        SavedTrialMatchCriterionDb high = DomainBuilderDatabase.getSavedTrialMatchCriterionDb(matchId, null);
        high.setScore(new BigDecimal("0.9000"));
        repository.save(high);

        // A different match's evidence must not leak in.
        repository.save(DomainBuilderDatabase.getSavedTrialMatchCriterionDb());

        List<SavedTrialMatchCriterionDb> found = repository.findByTrialMatchId(matchId);

        assertEquals(2, found.size());
        assertEquals(0, new BigDecimal("0.9000").compareTo(found.get(0).getScore()));
        assertEquals(0, new BigDecimal("0.4000").compareTo(found.get(1).getScore()));
    }

    @Test
    void findByTrialMatchId_shouldExcludeSoftDeleted() {
        Long matchId = 556L;

        repository.save(DomainBuilderDatabase.getSavedTrialMatchCriterionDb(matchId, null));

        SavedTrialMatchCriterionDb deleted = DomainBuilderDatabase.getSavedTrialMatchCriterionDb(matchId, null);
        deleted.setActive(ActiveEnum.INACTIVE);
        repository.save(deleted);

        List<SavedTrialMatchCriterionDb> found = repository.findByTrialMatchId(matchId);

        assertEquals(1, found.size());
        assertEquals(ActiveEnum.ACTIVE, found.get(0).getActive());
    }

    @Test
    void isExclusion_shouldPersist() {
        SavedTrialMatchCriterionDb exclusion = DomainBuilderDatabase.getSavedTrialMatchCriterionDb();
        exclusion.setIsExclusion(true);

        SavedTrialMatchCriterionDb saved = repository.save(exclusion);

        assertTrue(repository.findByExtid(saved.getExtid()).orElseThrow().getIsExclusion());
    }

    @Test
    void findByActive_shouldReturnOnlyActive() {
        repository.save(DomainBuilderDatabase.getSavedTrialMatchCriterionDb());

        SavedTrialMatchCriterionDb inactive = DomainBuilderDatabase.getSavedTrialMatchCriterionDb();
        inactive.setActive(ActiveEnum.INACTIVE);
        repository.save(inactive);

        Page<SavedTrialMatchCriterionDb> found = repository.findByActive(ActiveEnum.ACTIVE, PageRequest.of(0, 10));

        assertEquals(1, found.getTotalElements());
    }

    @Test
    void findByActive_shouldReturnOnlyInactive() {
        repository.save(DomainBuilderDatabase.getSavedTrialMatchCriterionDb());

        SavedTrialMatchCriterionDb inactive = DomainBuilderDatabase.getSavedTrialMatchCriterionDb();
        inactive.setActive(ActiveEnum.INACTIVE);
        repository.save(inactive);

        Page<SavedTrialMatchCriterionDb> found = repository.findByActive(ActiveEnum.INACTIVE, PageRequest.of(0, 10));

        assertEquals(1, found.getTotalElements());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        SavedTrialMatchCriterionDb saved = repository.save(DomainBuilderDatabase.getSavedTrialMatchCriterionDb());

        assertTrue(repository.existsByExtid(saved.getExtid()));
    }

    @Test
    void existsByExtid_shouldReturnFalse_whenNotExists() {
        assertFalse(repository.existsByExtid(UUID.randomUUID().toString()));
    }

    @Test
    void save_shouldPersistCriterion() {
        SavedTrialMatchCriterionDb saved = repository.save(DomainBuilderDatabase.getSavedTrialMatchCriterionDb());

        assertNotNull(saved.getId());
        assertNotNull(saved.getScore());
        assertNotNull(saved.getChunkText());
    }

    @Test
    void findAll_shouldReturnAllCriteria() {
        repository.save(DomainBuilderDatabase.getSavedTrialMatchCriterionDb());
        repository.save(DomainBuilderDatabase.getSavedTrialMatchCriterionDb());

        assertEquals(2, repository.findAll().size());
    }

    @Test
    void deleteById_shouldRemoveCriterion() {
        SavedTrialMatchCriterionDb saved = repository.save(DomainBuilderDatabase.getSavedTrialMatchCriterionDb());

        repository.deleteById(saved.getId());

        assertTrue(repository.findByExtid(saved.getExtid()).isEmpty());
    }
}
