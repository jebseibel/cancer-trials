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
     * @param query          natural-language query
     * @param maxTrials      how many distinct trials to return
     * @param recruitingOnly restrict to actively recruiting trials
     * @param excludeExclusionCriteria drop exclusion-criteria matches. Useful when asking
     *        "what might fit", since matching an exclusion means the opposite of qualifying.
     *        Leave false to see disqualifying matches too - often the more important answer.
     * @param similarityThreshold minimum score, 0..1. Below this a match is noise.
     */
    public List<TrialMatch> search(String query, int maxTrials, boolean recruitingOnly,
                                   boolean excludeExclusionCriteria, Double similarityThreshold) {
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

        String filter = buildFilter(recruitingOnly, excludeExclusionCriteria);
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
    private String buildFilter(boolean recruitingOnly, boolean excludeExclusionCriteria) {
        List<String> clauses = new ArrayList<>();
        if (recruitingOnly) {
            clauses.add("overallStatus == 'RECRUITING'");
        }
        if (excludeExclusionCriteria) {
            clauses.add("isExclusion == false");
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

            Trial trial = trialDbService.findByExtid(e.getKey());
            if (trial == null) {
                // Vector store is ahead of MySQL - a trial was deleted but its chunks remain.
                // Expected under eventual consistency; backfill reconciles it (section 6).
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
