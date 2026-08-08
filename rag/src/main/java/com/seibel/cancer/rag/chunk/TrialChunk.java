package com.seibel.cancer.rag.chunk;

import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.rag.chunk.EligibilityChunk.ChunkType;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One embeddable chunk of a trial, plus the metadata retrieval filters on.
 *
 * <p>Because Qdrant cannot join to MySQL, anything that must be filterable at search time
 * has to be duplicated here as metadata (RAG_PLAN.md section 7). Retrieval returns these
 * chunks, and the full trial record is then hydrated from MySQL by extid.
 *
 * @param trialExtid  the trial's extid - <b>never the numeric id</b>, per the project's
 *                    extid-only rule. This is the key used to hydrate from MySQL.
 * @param source      which field or child record the text came from
 * @param text        the embeddable text
 * @param ordinal     position within its source, 0-based, so a citation can say "the 3rd
 *                    inclusion criterion"
 * @param nctId       the CT.gov identifier, for linking back to the public listing
 * @param overallStatus trial status (e.g. RECRUITING) - the primary retrieval filter
 * @param studyType   interventional vs observational
 */
public record TrialChunk(
        String trialExtid,
        Source source,
        String text,
        int ordinal,
        String nctId,
        String overallStatus,
        String studyType
) {

    /** Where a chunk's text came from. Doubles as a retrieval filter dimension. */
    public enum Source {
        INCLUSION_CRITERION,
        /** Matching one of these is <b>disqualifying</b>, not qualifying - the distinction must survive into metadata. */
        EXCLUSION_CRITERION,
        /** Eligibility text with no inclusion/exclusion headers; see EligibilityChunk.ChunkType.UNPARSED. */
        ELIGIBILITY_UNPARSED,
        BRIEF_SUMMARY,
        DETAILED_DESCRIPTION,
        INTERVENTION,
        OUTCOME
    }

    static TrialChunk of(Trial trial, Source source, String text, int ordinal, ChunkType ignored) {
        return new TrialChunk(
                trial.getExtid(),
                source,
                text,
                ordinal,
                trial.getNctId(),
                trial.getOverallStatus(),
                trial.getStudyType());
    }

    /**
     * A stable, deterministic id for this chunk, as a UUID.
     *
     * <p>Deterministic so re-indexing the same trial overwrites rather than duplicates -
     * backfill is run repeatedly (after chunking changes, after failures, after an
     * embedding-model swap), so it must be idempotent.
     *
     * <p>Must be a UUID or an unsigned integer: Qdrant rejects arbitrary strings as point
     * ids ("UUID string too large"). So the readable natural key is hashed into a v3-style
     * UUID via {@link UUID#nameUUIDFromBytes} - content-addressed, so the same chunk always
     * maps to the same id, which is what keeps re-indexing idempotent.
     */
    public String id() {
        return UUID.nameUUIDFromBytes(naturalKey().getBytes(StandardCharsets.UTF_8)).toString();
    }

    /** The human-readable key the id is derived from. Useful for debugging. */
    public String naturalKey() {
        return trialExtid + ":" + source + ":" + ordinal;
    }

    /** Metadata map for the vector store, used to build retrieval filter expressions. */
    public Map<String, Object> metadata() {
        Map<String, Object> m = new HashMap<>();
        m.put("trialExtid", trialExtid);
        m.put("source", source.name());
        m.put("ordinal", ordinal);
        // The point id is a hashed UUID, which is opaque when inspecting the store. Keep the
        // readable key alongside it so a stored point can be traced back to its origin.
        m.put("naturalKey", naturalKey());
        // Lets a query exclude exclusion-criteria matches, which mean the opposite of a fit.
        m.put("isExclusion", source == Source.EXCLUSION_CRITERION);
        if (nctId != null) m.put("nctId", nctId);
        if (overallStatus != null) m.put("overallStatus", overallStatus);
        if (studyType != null) m.put("studyType", studyType);
        return m;
    }
}
