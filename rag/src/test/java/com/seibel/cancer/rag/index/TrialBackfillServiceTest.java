package com.seibel.cancer.rag.index;

import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.common.enums.ActiveEnum;
import com.seibel.cancer.database.db.service.TrialDbService;
import com.seibel.cancer.rag.config.RagProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the skip-already-indexed branch and its override.
 *
 * <p>The distinction under test is the one that makes the skip safe: presence of chunks means
 * "already done" only while the chunker and embedding model are unchanged, so {@code force} has
 * to reach every trial regardless of what the store holds.
 */
class TrialBackfillServiceTest {

    private TrialDbService trialDbService;
    private TrialIndexService indexService;
    private TrialBackfillService backfillService;

    @BeforeEach
    void setUp() {
        trialDbService = mock(TrialDbService.class);
        indexService = mock(TrialIndexService.class);

        RagProperties properties = new RagProperties();
        backfillService = new TrialBackfillService(trialDbService, indexService, properties);

        // Null means "usable" - the readiness probe is not what these tests are about.
        when(indexService.checkVectorStoreReady()).thenReturn(null);
    }

    private Trial trial(String extid, String nctId) {
        Trial t = new Trial();
        t.setExtid(extid);
        t.setNctId(nctId);
        return t;
    }

    /** One page holding the given trials, with no next page. */
    private void givenTrials(Trial... trials) {
        Page<Trial> page = new PageImpl<>(List.of(trials), PageRequest.of(0, 25), trials.length);
        when(trialDbService.findByActive(eq(ActiveEnum.ACTIVE), any(Pageable.class))).thenReturn(page);
    }

    @Test
    @DisplayName("skips trials the vector store already holds chunks for")
    void skipsAlreadyIndexedTrials() {
        Trial indexed = trial("extid-1", "NCT00000001");
        Trial fresh = trial("extid-2", "NCT00000002");
        givenTrials(indexed, fresh);

        when(indexService.isIndexed("extid-1")).thenReturn(true);
        when(indexService.isIndexed("extid-2")).thenReturn(false);
        when(indexService.reindexTrial("extid-2")).thenReturn(7);

        var result = backfillService.backfillAll(25, Integer.MAX_VALUE);

        assertThat(result.trialsAlreadyIndexed()).isEqualTo(1);
        assertThat(result.trialsIndexed()).isEqualTo(1);
        assertThat(result.chunksWritten()).isEqualTo(7);
        verify(indexService, never()).reindexTrial("extid-1");
    }

    @Test
    @DisplayName("force re-embeds trials that are already indexed")
    void forceReindexesEverything() {
        Trial indexed = trial("extid-1", "NCT00000001");
        givenTrials(indexed);

        when(indexService.reindexTrial("extid-1")).thenReturn(5);

        var result = backfillService.backfillAll(25, Integer.MAX_VALUE, true);

        assertThat(result.trialsAlreadyIndexed()).isZero();
        assertThat(result.trialsIndexed()).isEqualTo(1);
        // The probe must not even run under force - a stale-vector re-index cannot be
        // conditional on what the store currently holds.
        verify(indexService, never()).isIndexed(anyString());
    }

    @Test
    @DisplayName("counts no-chunk trials separately from already-indexed ones")
    void separatesTheTwoSkipReasons() {
        Trial empty = trial("extid-1", "NCT00000001");
        Trial indexed = trial("extid-2", "NCT00000002");
        givenTrials(empty, indexed);

        when(indexService.isIndexed("extid-1")).thenReturn(false);
        when(indexService.reindexTrial("extid-1")).thenReturn(0);
        when(indexService.isIndexed("extid-2")).thenReturn(true);

        var result = backfillService.backfillAll(25, Integer.MAX_VALUE);

        assertThat(result.trialsSkipped()).isEqualTo(1);
        assertThat(result.trialsAlreadyIndexed()).isEqualTo(1);
        assertThat(result.trialsIndexed()).isZero();
    }

    @Test
    @DisplayName("already-indexed trials count toward maxTrials")
    void alreadyIndexedCountsTowardMaxTrials() {
        Trial first = trial("extid-1", "NCT00000001");
        Trial second = trial("extid-2", "NCT00000002");
        givenTrials(first, second);

        when(indexService.isIndexed("extid-1")).thenReturn(true);

        var result = backfillService.backfillAll(25, 1);

        // Without this, a bounded run would scan the whole corpus hunting for unindexed trials
        // instead of stopping where it was told to.
        assertThat(result.trialsAlreadyIndexed()).isEqualTo(1);
        verify(indexService, never()).isIndexed("extid-2");
    }

    @Test
    @DisplayName("aborts with a setup message when the vector store is unusable")
    void abortsWhenStoreUnavailable() {
        when(indexService.checkVectorStoreReady()).thenReturn("collection missing");

        var result = backfillService.backfillAll(25, Integer.MAX_VALUE);

        assertThat(result.errors()).containsExactly("collection missing");
        assertThat(result.trialsIndexed()).isZero();
        assertThat(result.trialsAlreadyIndexed()).isZero();
        verify(trialDbService, never()).findByActive(any(), any());
    }
}
