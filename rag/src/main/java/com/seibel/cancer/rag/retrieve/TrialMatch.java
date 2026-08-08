package com.seibel.cancer.rag.retrieve;

import com.seibel.cancer.common.domain.Trial;

import java.util.List;

/**
 * One trial that matched a query, with the specific chunks that matched it.
 *
 * <p>Grouped by trial rather than returned as a flat chunk list because a single trial often
 * matches on several criteria at once, and the useful answer is "this trial, and here is why"
 * rather than five near-identical rows.
 *
 * <p>The matched chunks are what make grounding possible: an answer can cite the exact
 * eligibility line it relied on, which RAG_PLAN.md section 9 requires so criteria can be
 * checked against the original listing.
 *
 * @param trial      the full normalized record, hydrated from MySQL by extid
 * @param topScore   best similarity score among this trial's matched chunks
 * @param matches    the matched chunks, best first
 */
public record TrialMatch(Trial trial, double topScore, List<ChunkMatch> matches) {

    /**
     * A single matched chunk.
     *
     * @param text     the matched text - what a citation quotes
     * @param source   which field it came from (inclusion criterion, outcome, ...)
     * @param ordinal  position within that source, so a citation can say "3rd inclusion criterion"
     * @param score    similarity score, higher is closer
     * @param isExclusion whether this is an <b>exclusion</b> criterion. A high-scoring
     *                    exclusion match means the patient may be <em>disqualified</em> - the
     *                    opposite of a fit - so callers must not treat it as evidence of
     *                    eligibility.
     */
    public record ChunkMatch(String text, String source, int ordinal, double score, boolean isExclusion) {
    }
}
