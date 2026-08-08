package com.seibel.cancer.web.controller;

import com.seibel.cancer.rag.index.TrialBackfillService;
import com.seibel.cancer.rag.index.TrialIndexService;
import com.seibel.cancer.rag.retrieve.TrialRetrievalService;
import com.seibel.cancer.web.response.ResponseBackfillResult;
import com.seibel.cancer.web.response.ResponseTrialSearchMatch;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * On-demand vector-store indexing.
 *
 * <p>Backfill is not one-off setup - it is the reconciliation mechanism for the
 * eventual-consistency gap between MySQL and the vector store, re-run after chunking
 * changes, after indexing failures, and after an embedding-model swap (RAG_PLAN.md
 * section 6).
 */
@RestController
@RequestMapping("/api/rag")
@Validated
@Tag(name = "RAG Index", description = "Vector-store indexing and backfill")
@RequiredArgsConstructor
public class RagIndexController {

    private final TrialBackfillService backfillService;
    private final TrialIndexService indexService;
    private final TrialRetrievalService retrievalService;

    @PostMapping("/backfill")
    @Operation(summary = "Chunk, embed, and index trials already in the database")
    public ResponseBackfillResult backfill(
            // Omit pageSize to use cancer.rag.backfill.page-size.
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(defaultValue = "2147483647") int maxTrials) {

        var result = pageSize == null
                ? backfillService.backfillAll(maxTrials)
                : backfillService.backfillAll(pageSize, maxTrials);

        return ResponseBackfillResult.builder()
                .trialsIndexed(result.trialsIndexed())
                .chunksWritten(result.chunksWritten())
                .trialsSkipped(result.trialsSkipped())
                .errors(result.errors())
                .build();
    }

    @GetMapping("/search")
    @Operation(summary = "Semantic search over trial chunks, with optional metadata filters")
    public java.util.List<ResponseTrialSearchMatch> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int maxTrials,
            @RequestParam(defaultValue = "false") boolean recruitingOnly,
            @RequestParam(defaultValue = "false") boolean excludeExclusionCriteria,
            // No default: omitting it falls back to cancer.rag.retrieval.default-similarity-threshold
            // rather than hardcoding 0.0 here.
            @RequestParam(required = false) Double similarityThreshold) {

        return retrievalService
                .search(query, maxTrials, recruitingOnly, excludeExclusionCriteria, similarityThreshold)
                .stream()
                .map(m -> ResponseTrialSearchMatch.builder()
                        .trialExtid(m.trial().getExtid())
                        .nctId(m.trial().getNctId())
                        .briefTitle(m.trial().getBriefTitle())
                        .overallStatus(m.trial().getOverallStatus())
                        .topScore(m.topScore())
                        .matches(m.matches().stream()
                                .map(c -> ResponseTrialSearchMatch.Match.builder()
                                        .text(c.text())
                                        .source(c.source())
                                        .ordinal(c.ordinal())
                                        .score(c.score())
                                        .isExclusion(c.isExclusion())
                                        .build())
                                .toList())
                        .build())
                .toList();
    }

    @PostMapping("/reindex/{trialExtid}")
    @Operation(summary = "Re-index a single trial by extid")
    public ResponseBackfillResult reindexOne(@PathVariable String trialExtid) {
        int written = indexService.reindexTrial(trialExtid);
        return ResponseBackfillResult.builder()
                .trialsIndexed(written > 0 ? 1 : 0)
                .chunksWritten(written)
                .trialsSkipped(written == 0 ? 1 : 0)
                .errors(java.util.List.of())
                .build();
    }
}
