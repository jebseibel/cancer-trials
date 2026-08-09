package com.seibel.cancer.database.db.repository;

import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.entity.SavedTrialMatchDb;
import com.seibel.cancer.testutils.DomainBuilderDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

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
class TrialMatchRepositoryTest {

    @Autowired
    private SavedTrialMatchRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void findByExtid_shouldReturnTrialMatch_whenExists() {
        SavedTrialMatchDb saved = repository.save(DomainBuilderDatabase.getSavedTrialMatchDb());

        Optional<SavedTrialMatchDb> found = repository.findByExtid(saved.getExtid());

        assertTrue(found.isPresent());
        assertEquals(saved.getExtid(), found.get().getExtid());
    }

    @Test
    void findByExtid_shouldReturnEmpty_whenNotExists() {
        assertTrue(repository.findByExtid(UUID.randomUUID().toString()).isEmpty());
    }

    @Test
    void findBySearchRunId_shouldReturnMatchesInRankOrder() {
        String runId = UUID.randomUUID().toString();

        SavedTrialMatchDb second = DomainBuilderDatabase.getSavedTrialMatchDb(null, runId);
        second.setMatchRank(2);
        repository.save(second);

        SavedTrialMatchDb first = DomainBuilderDatabase.getSavedTrialMatchDb(null, runId);
        first.setMatchRank(1);
        repository.save(first);

        // A different run must not leak into this one's results.
        repository.save(DomainBuilderDatabase.getSavedTrialMatchDb());

        List<SavedTrialMatchDb> found = repository.findBySearchRunId(runId);

        assertEquals(2, found.size());
        assertEquals(1, found.get(0).getMatchRank());
        assertEquals(2, found.get(1).getMatchRank());
    }

    @Test
    void findBySearchRunId_shouldExcludeSoftDeleted() {
        String runId = UUID.randomUUID().toString();

        repository.save(DomainBuilderDatabase.getSavedTrialMatchDb(null, runId));

        SavedTrialMatchDb deleted = DomainBuilderDatabase.getSavedTrialMatchDb(null, runId);
        deleted.setActive(ActiveEnum.INACTIVE);
        repository.save(deleted);

        List<SavedTrialMatchDb> found = repository.findBySearchRunId(runId);

        assertEquals(1, found.size());
        assertEquals(ActiveEnum.ACTIVE, found.get(0).getActive());
    }

    @Test
    void findByAppUserId_shouldReturnMatchesForThatUserOnly() {
        Long appUserId = 4242L;

        SavedTrialMatchDb mine = DomainBuilderDatabase.getSavedTrialMatchDb();
        mine.setAppUserId(appUserId);
        repository.save(mine);

        repository.save(DomainBuilderDatabase.getSavedTrialMatchDb());

        List<SavedTrialMatchDb> found = repository.findByAppUserId(appUserId);

        assertEquals(1, found.size());
        assertEquals(appUserId, found.get(0).getAppUserId());
    }

    @Test
    void findByTrialId_shouldReturnEveryRunThatTrialAppearedIn() {
        Long trialId = 777L;

        repository.save(DomainBuilderDatabase.getSavedTrialMatchDb(trialId, null));
        repository.save(DomainBuilderDatabase.getSavedTrialMatchDb(trialId, null));
        repository.save(DomainBuilderDatabase.getSavedTrialMatchDb());

        List<SavedTrialMatchDb> found = repository.findByTrialId(trialId);

        assertEquals(2, found.size());
        assertTrue(found.stream().allMatch(m -> trialId.equals(m.getTrialId())));
    }

    @Test
    void findByActive_shouldReturnOnlyActive() {
        repository.save(DomainBuilderDatabase.getSavedTrialMatchDb());

        SavedTrialMatchDb inactive = DomainBuilderDatabase.getSavedTrialMatchDb();
        inactive.setActive(ActiveEnum.INACTIVE);
        repository.save(inactive);

        Page<SavedTrialMatchDb> found = repository.findByActive(ActiveEnum.ACTIVE, PageRequest.of(0, 10));

        assertEquals(1, found.getTotalElements());
    }

    @Test
    void findByActive_shouldReturnOnlyInactive() {
        repository.save(DomainBuilderDatabase.getSavedTrialMatchDb());

        SavedTrialMatchDb inactive = DomainBuilderDatabase.getSavedTrialMatchDb();
        inactive.setActive(ActiveEnum.INACTIVE);
        repository.save(inactive);

        Page<SavedTrialMatchDb> found = repository.findByActive(ActiveEnum.INACTIVE, PageRequest.of(0, 10));

        assertEquals(1, found.getTotalElements());
    }

    @Test
    void existsByExtid_shouldReturnTrue_whenExists() {
        SavedTrialMatchDb saved = repository.save(DomainBuilderDatabase.getSavedTrialMatchDb());

        assertTrue(repository.existsByExtid(saved.getExtid()));
    }

    @Test
    void existsByExtid_shouldReturnFalse_whenNotExists() {
        assertFalse(repository.existsByExtid(UUID.randomUUID().toString()));
    }

    @Test
    void save_shouldPersistTrialMatch() {
        SavedTrialMatchDb saved = repository.save(DomainBuilderDatabase.getSavedTrialMatchDb());

        assertNotNull(saved.getId());
        assertNotNull(saved.getTopScore());
        assertNotNull(saved.getMatchedAt());
    }

    @Test
    void findAll_shouldReturnAllTrialMatches() {
        repository.save(DomainBuilderDatabase.getSavedTrialMatchDb());
        repository.save(DomainBuilderDatabase.getSavedTrialMatchDb());

        assertEquals(2, repository.findAll().size());
    }

    @Test
    void deleteById_shouldRemoveTrialMatch() {
        SavedTrialMatchDb saved = repository.save(DomainBuilderDatabase.getSavedTrialMatchDb());

        repository.deleteById(saved.getId());

        assertTrue(repository.findByExtid(saved.getExtid()).isEmpty());
    }
}
