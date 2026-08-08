package com.seibel.cancer.rag.chunk;

/**
 * One embeddable unit of eligibility text, plus the metadata retrieval filters on.
 *
 * @param text     the criterion text, unescaped and with bullet markers stripped. For a
 *                 nested item this includes its parent line as a prefix - "age >= 60
 *                 years" is not retrievable on its own, but "Female patients must meet one
 *                 of the following: age >= 60 years" is.
 * @param type     inclusion, exclusion, or unparsed
 * @param ordinal  position within its section, 0-based. Preserves document order so a
 *                 citation can point at "the 3rd inclusion criterion".
 */
public record EligibilityChunk(String text, ChunkType type, int ordinal) {

    public enum ChunkType {
        /** From an "Inclusion Criteria" section. */
        INCLUSION,
        /** From an "Exclusion Criteria" section. Matching one is disqualifying, not qualifying. */
        EXCLUSION,
        /**
         * No inclusion/exclusion headers were found, so the whole block is emitted as one
         * chunk rather than being guessed at. Roughly 4% of a 50-trial sample - mostly
         * older records using the legacy "DISEASE CHARACTERISTICS:" convention. Counting
         * these is a quality metric: a rising share means the parser is falling behind the
         * corpus (see RAG_PLAN.md section 5).
         */
        UNPARSED
    }
}
