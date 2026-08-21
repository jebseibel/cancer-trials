package com.seibel.cancer.rag.retrieve;

import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.database.db.service.TrialDbService;
import com.seibel.cancer.rag.config.RagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Semantic search over trial chunks, combined with metadata filtering.
 *
 * <p>Two-phase by necessity: the vector store cannot join to MySQL, so retrieval returns
 * chunk matches carrying trial extids, and the full records are then hydrated from MySQL
 * (RAG_PLAN.md section 7). Anything that must be filterable at search time has to live in
 * chunk metadata, which is why {@code TrialChunk} duplicates status and source there.
 *
 * <p>Written against Spring AI's {@link VectorStore} interface, never Qdrant classes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrialRetrievalService {

    private final VectorStore vectorStore;
    private final TrialDbService trialDbService;
    private final RagProperties ragProperties;

    /**
     * Chunk sources that state who may enrol, as opposed to what the trial is or does.
     *
     * <p>{@code ELIGIBILITY_UNPARSED} belongs here: it is criteria text the chunker could not
     * split into sections, not prose. Excluding it would silently drop the ~5% of trials whose
     * criteria carry no section header.
     */
    private static final String CRITERIA_SOURCES_CLAUSE =
            "(source == 'INCLUSION_CRITERION'"
                    + " || source == 'EXCLUSION_CRITERION'"
                    + " || source == 'ELIGIBILITY_UNPARSED')";

    /**
     * @param query          natural-language query
     * @param maxTrials      how many distinct trials to return
     * @param recruitingOnly restrict to actively recruiting trials
     * @param excludeExclusionCriteria drop exclusion-criteria matches. Useful when asking
     *        "what might fit", since matching an exclusion means the opposite of qualifying.
     *        Leave false to see disqualifying matches too - often the more important answer.
     * @param criteriaOnly restrict to eligibility-criteria chunks, dropping titles, summaries,
     *        descriptions, interventions and outcomes. Measured 2026-08-21: on a whole-profile
     *        query, 15 of the top 25 hits were trial-design prose ("This is a first-in-human,
     *        open-label, phase I/Ib study...") repeated across unrelated trials, which crowds
     *        out the criteria that decide whether a patient qualifies. Filtering removes them.
     *        <p>Deliberately not the default: a query like "what is this trial testing" is
     *        answered by the summary, and defaulting to true would silently break it.
     * @param similarityThreshold minimum score, 0..1. Below this a match is noise.
     */
    public List<TrialMatch> search(String query, int maxTrials, boolean recruitingOnly,
                                   boolean excludeExclusionCriteria, boolean criteriaOnly,
                                   Double similarityThreshold) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        // Null means "not specified by the caller" - fall back to the configured default rather
        // than silently assuming 0.
        double threshold = similarityThreshold != null
                ? similarityThreshold
                : ragProperties.getRetrieval().getDefaultSimilarityThreshold();

        // Over-fetch: several chunks routinely belong to the same trial, so fetching exactly
        // maxTrials chunks would yield far fewer than maxTrials distinct trials.
        int multiplier = ragProperties.getRetrieval().getChunkFetchMultiplier();

        SearchRequest.Builder request = SearchRequest.builder()
                .query(query)
                .topK(Math.max(maxTrials, 1) * multiplier)
                .similarityThreshold(threshold);

        String filter = buildFilter(recruitingOnly, excludeExclusionCriteria, criteriaOnly);
        if (filter != null) {
            request.filterExpression(filter);
        }

        List<Document> hits = vectorStore.similaritySearch(request.build());
        if (hits == null || hits.isEmpty()) {
            return List.of();
        }

        return groupAndHydrate(hits, maxTrials);
    }

    /**
     * Builds a Spring AI portable filter expression, which the store translates to its own
     * native filter syntax.
     */
    private String buildFilter(boolean recruitingOnly, boolean excludeExclusionCriteria,
                               boolean criteriaOnly) {
        List<String> clauses = new ArrayList<>();
        if (recruitingOnly) {
            clauses.add("overallStatus == 'RECRUITING'");
        }
        if (excludeExclusionCriteria) {
            clauses.add("isExclusion == false");
        }
        if (criteriaOnly) {
            // OR-of-equals rather than `source in [...]`. Both parse, but == is the operator
            // the two clauses above already use against this store, and IN's translation to
            // Qdrant's native filter is the thing that was not proven when this was written.
            clauses.add(CRITERIA_SOURCES_CLAUSE);
        }
        return clauses.isEmpty() ? null : String.join(" && ", clauses);
    }

    /** Groups chunk hits by trial, then loads each trial's full record from MySQL. */
    private List<TrialMatch> groupAndHydrate(List<Document> hits, int maxTrials) {
        // LinkedHashMap preserves similarity order - the first hit for a trial is its best.
        Map<String, List<Document>> byTrial = new LinkedHashMap<>();
        for (Document d : hits) {
            Object extid = d.getMetadata().get("trialExtid");
            if (extid == null) continue;
            byTrial.computeIfAbsent(extid.toString(), k -> new ArrayList<>()).add(d);
        }

        List<TrialMatch> results = new ArrayList<>();
        for (Map.Entry<String, List<Document>> e : byTrial.entrySet()) {
            if (results.size() >= maxTrials) break;

            // The vector store routinely holds chunks for trials MySQL no longer has - a
            // database rebuild invalidates every extid while the collection keeps its points,
            // and nothing reconciles the two. Skipping one orphan must not fail the search.
            //
            // findByExtid throws rather than returning null, so this has to be a catch: the
            // null guard that used to stand here could never fire, and one stale chunk turned
            // the whole request into a 500.
            Trial trial;
            try {
                trial = trialDbService.findByExtid(e.getKey());
            } catch (RuntimeException ex) {
                log.warn("retrieval: chunks reference missing trial extid={}", e.getKey());
                continue;
            }

            List<TrialMatch.ChunkMatch> matches = e.getValue().stream()
                    .map(this::toChunkMatch)
                    .sorted(Comparator.comparingDouble(TrialMatch.ChunkMatch::score).reversed())
                    .toList();

            double topScore = matches.isEmpty() ? 0.0 : matches.getFirst().score();
            results.add(new TrialMatch(trial, topScore, matches));
        }

        results.sort(Comparator.comparingDouble(TrialMatch::topScore).reversed());
        return results;
    }

    private TrialMatch.ChunkMatch toChunkMatch(Document d) {
        Map<String, Object> m = d.getMetadata();
        return new TrialMatch.ChunkMatch(
                d.getText(),
                String.valueOf(m.getOrDefault("source", "UNKNOWN")),
                m.get("ordinal") instanceof Number n ? n.intValue() : 0,
                d.getScore() == null ? 0.0 : d.getScore(),
                Boolean.TRUE.equals(m.get("isExclusion")));
    }
}
