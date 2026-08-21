package com.seibel.cancer.rag.retrieve;

import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.common.exceptions.ServiceException;
import com.seibel.cancer.database.db.service.TrialDbService;
import com.seibel.cancer.rag.config.RagProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the metadata filter expression, which is the only part of retrieval that decides what
 * the store is even allowed to return.
 *
 * <p>These assert on the filter string handed to the {@link VectorStore} rather than on results.
 * Spring AI translates that expression into each store's native syntax, so the expression is the
 * contract this service owns; whether Qdrant honours it is Spring AI's concern and is covered by
 * a live run, not a unit test.
 */
class TrialRetrievalServiceTest {

    private VectorStore vectorStore;
    private TrialDbService trialDbService;
    private TrialRetrievalService service;

    @BeforeEach
    void setUp() {
        vectorStore = mock(VectorStore.class);
        trialDbService = mock(TrialDbService.class);
        service = new TrialRetrievalService(vectorStore, trialDbService, new RagProperties());

        // No hits: these tests are about the request, not what comes back.
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
    }

    /** Runs a search and returns the filter expression the store was asked for, possibly null. */
    private String filterFor(boolean recruitingOnly, boolean excludeExclusionCriteria,
                             boolean criteriaOnly) {
        service.search("any query", 5, recruitingOnly, excludeExclusionCriteria, criteriaOnly, null);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        return captor.getValue().getFilterExpression() == null
                ? null
                : captor.getValue().getFilterExpression().toString();
    }

    @Test
    @DisplayName("no filters requested leaves the expression unset")
    void noFiltersMeansNoExpression() {
        assertThat(filterFor(false, false, false)).isNull();
    }

    @Test
    @DisplayName("criteriaOnly restricts to the three criteria sources")
    void criteriaOnlyRestrictsSources() {
        String filter = filterFor(false, false, true);

        assertThat(filter)
                .contains("INCLUSION_CRITERION")
                .contains("EXCLUSION_CRITERION")
                .contains("ELIGIBILITY_UNPARSED");
    }

    /**
     * The unparsed source is criteria text the chunker could not split, not prose. Dropping it
     * would silently lose the ~5% of trials whose criteria carry no section header - the exact
     * trials where a reader most needs to see the raw text.
     */
    @Test
    @DisplayName("criteriaOnly keeps unparsed criteria rather than treating them as prose")
    void criteriaOnlyKeepsUnparsed() {
        assertThat(filterFor(false, false, true)).contains("ELIGIBILITY_UNPARSED");
    }

    @Test
    @DisplayName("criteriaOnly never admits prose or outcome sources")
    void criteriaOnlyExcludesProse() {
        String filter = filterFor(false, false, true);

        assertThat(filter)
                .doesNotContain("BRIEF_SUMMARY")
                .doesNotContain("DETAILED_DESCRIPTION")
                .doesNotContain("OUTCOME")
                .doesNotContain("INTERVENTION");
    }

    @Test
    @DisplayName("criteriaOnly combines with the existing filters rather than replacing them")
    void criteriaOnlyCombinesWithOtherFilters() {
        String filter = filterFor(true, true, true);

        assertThat(filter)
                .contains("RECRUITING")
                .contains("isExclusion")
                .contains("INCLUSION_CRITERION");
    }

    /**
     * Prose is the right answer to "what is this trial testing", so the caller has to ask for the
     * restriction. A default of true would silently break those queries.
     */
    @Test
    @DisplayName("leaving criteriaOnly off does not restrict by source")
    void criteriaOnlyIsOptIn() {
        assertThat(filterFor(true, false, false))
                .doesNotContain("INCLUSION_CRITERION");
    }

    @Test
    @DisplayName("a blank query never reaches the store")
    void blankQueryShortCircuits() {
        assertThat(service.search("  ", 5, false, false, true, null)).isEmpty();
        verify(vectorStore, never()).similaritySearch(any(SearchRequest.class));
    }

    /** A hit whose trial extid is the given one. */
    private Document hitFor(String trialExtid) {
        return Document.builder()
                .text("some matched criterion")
                .metadata(Map.of(
                        "trialExtid", trialExtid,
                        "source", "INCLUSION_CRITERION",
                        "ordinal", 0,
                        "isExclusion", false))
                .score(0.9)
                .build();
    }

    /**
     * A rebuild invalidates every trial extid while the vector collection keeps its points, so
     * chunks pointing at trials MySQL no longer has are routine rather than exceptional.
     *
     * <p>{@code findByExtid} throws instead of returning null, so a stale chunk used to escape
     * as a 500 and fail the whole search. One orphan must cost one result, not all of them.
     */
    @Test
    @DisplayName("a chunk referencing a deleted trial is skipped, not fatal")
    void orphanChunkIsSkipped() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(hitFor("gone"), hitFor("still-here")));

        when(trialDbService.findByExtid("gone"))
                .thenThrow(new ServiceException("TrialDb with extid=gone not found"));

        Trial present = new Trial();
        present.setExtid("still-here");
        present.setNctId("NCT00000001");
        when(trialDbService.findByExtid("still-here")).thenReturn(present);

        List<TrialMatch> results = service.search("any query", 5, false, false, false, null);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().trial().getExtid()).isEqualTo("still-here");
    }

    @Test
    @DisplayName("every trial being an orphan returns empty rather than throwing")
    void allOrphansReturnsEmpty() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(hitFor("gone-a"), hitFor("gone-b")));
        when(trialDbService.findByExtid(anyString()))
                .thenThrow(new ServiceException("not found"));

        assertThat(service.search("any query", 5, false, false, true, null)).isEmpty();
    }
}
