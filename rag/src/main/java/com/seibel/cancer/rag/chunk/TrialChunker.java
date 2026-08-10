package com.seibel.cancer.rag.chunk;

import com.seibel.cancer.common.domain.Intervention;
import com.seibel.cancer.common.domain.Outcome;
import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.rag.chunk.EligibilityChunk.ChunkType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a Trial plus its child records into the full set of embeddable chunks, with the
 * metadata retrieval filters on.
 *
 * <p>Chunk types follow RAG_PLAN.md section 5 - one per semantic unit, so a match points at
 * a specific criterion or intervention rather than a wall of text. Eligibility criteria are
 * delegated to {@link EligibilityCriteriaChunker}, which handles the messy real-world
 * shapes; everything else here is a straightforward field-to-chunk mapping.
 *
 * <p>Metadata carries <b>extid, never the numeric id</b>, per the project's extid-only rule -
 * an internal id must not reach the vector store any more than it reaches the frontend.
 *
 * <p>No Spring dependency: domain objects in, chunks out.
 */
public class TrialChunker {

    /** Default, used by the no-arg constructor. Overridden from {@code cancer.rag.chunking.*}. */
    private static final int DEFAULT_MAX_PROSE_CHUNK_CHARS = 900;

    private final EligibilityCriteriaChunker eligibilityChunker;

    /**
     * Long free-text fields are split on blank lines rather than embedded whole. Tuned to the
     * local embedding model's window - all-MiniLM-L6-v2 handles ~256 word pieces (~1000 chars),
     * so a 5000-char description embedded as one chunk is silently truncated past that point.
     * Raise this when moving to a model with a longer window.
     */
    private final int maxProseChunkChars;

    public TrialChunker() {
        this(new EligibilityCriteriaChunker(), DEFAULT_MAX_PROSE_CHUNK_CHARS);
    }

    public TrialChunker(EligibilityCriteriaChunker eligibilityChunker, int maxProseChunkChars) {
        this.eligibilityChunker = eligibilityChunker;
        this.maxProseChunkChars = maxProseChunkChars;
    }

    public List<TrialChunk> chunk(Trial trial, List<Intervention> interventions, List<Outcome> outcomes) {
        if (trial == null || trial.getExtid() == null) {
            return List.of();
        }
        List<TrialChunk> chunks = new ArrayList<>();

        // --- Eligibility criteria: one chunk per criterion ---
        //
        // Renumbered per source across the whole trial rather than reusing the eligibility
        // chunker's ordinal, which restarts at 0 for each criteria section. A trial with two
        // sections - e.g. NCT07393529, which has separate Patients and Social Network Members
        // blocks, each with its own Inclusion/Exclusion lists - then produces two
        // INCLUSION_CRITERION chunks at ordinal 0. Since a chunk id hashes
        // trialExtid:source:ordinal, those collide, and the store deduplicates by id before
        // embedding: 15 documents in, 10 embeddings back, and the add() call fails the whole
        // trial with "Embeddings must have the same number as that of the documents".
        Map<TrialChunk.Source, Integer> nextOrdinal = new EnumMap<>(TrialChunk.Source.class);
        for (EligibilityChunk ec : eligibilityChunker.chunk(trial.getEligibilityCriteria())) {
            TrialChunk.Source source = sourceFor(ec.type());
            int ordinal = nextOrdinal.merge(source, 1, Integer::sum) - 1;
            chunks.add(TrialChunk.of(trial, source, ec.text(), ordinal, ec.type()));
        }

        // --- Whole-trial prose ---
        addProse(chunks, trial, TrialChunk.Source.BRIEF_SUMMARY, trial.getBriefSummary());
        addProse(chunks, trial, TrialChunk.Source.DETAILED_DESCRIPTION, trial.getDetailedDescription());

        // --- Children ---
        int ordinal = 0;
        for (Intervention i : nullSafe(interventions)) {
            String text = joinNonBlank(i.getType(), i.getName(), i.getDescription());
            if (!text.isBlank()) {
                chunks.add(TrialChunk.of(trial, TrialChunk.Source.INTERVENTION, text, ordinal++, null));
            }
        }
        ordinal = 0;
        for (Outcome o : nullSafe(outcomes)) {
            String text = joinNonBlank(o.getOutcomeType(), o.getMeasure(), o.getDescription());
            if (!text.isBlank()) {
                chunks.add(TrialChunk.of(trial, TrialChunk.Source.OUTCOME, text, ordinal++, null));
            }
        }

        return chunks;
    }

    private TrialChunk.Source sourceFor(ChunkType type) {
        return switch (type) {
            case INCLUSION -> TrialChunk.Source.INCLUSION_CRITERION;
            case EXCLUSION -> TrialChunk.Source.EXCLUSION_CRITERION;
            case UNPARSED -> TrialChunk.Source.ELIGIBILITY_UNPARSED;
        };
    }

    /** Splits prose on blank lines, then packs paragraphs up to the window size. */
    private void addProse(List<TrialChunk> chunks, Trial trial, TrialChunk.Source source, String text) {
        if (text == null || text.isBlank()) return;

        int ordinal = 0;
        StringBuilder buf = new StringBuilder();
        for (String para : text.split("\\n\\s*\\n")) {
            String p = para.replaceAll("\\s+", " ").strip();
            if (p.isEmpty()) continue;
            if (!buf.isEmpty() && buf.length() + p.length() + 1 > maxProseChunkChars) {
                chunks.add(TrialChunk.of(trial, source, buf.toString(), ordinal++, null));
                buf.setLength(0);
            }
            if (!buf.isEmpty()) buf.append(' ');
            buf.append(p);
        }
        if (!buf.isEmpty()) {
            chunks.add(TrialChunk.of(trial, source, buf.toString(), ordinal, null));
        }
    }

    private String joinNonBlank(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            if (!sb.isEmpty()) sb.append(": ");
            sb.append(p.strip());
        }
        return sb.toString();
    }

    private <T> List<T> nullSafe(List<T> list) {
        return list == null ? List.of() : list;
    }
}
