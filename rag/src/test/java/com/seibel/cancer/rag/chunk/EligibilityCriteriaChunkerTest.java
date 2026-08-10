package com.seibel.cancer.rag.chunk;

import com.seibel.cancer.rag.chunk.EligibilityChunk.ChunkType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Driven by real eligibility text captured through this project's own ingestion pipeline
 * (see rag/src/test/resources/eligibility/README.md), not by hand-written samples. Each
 * fixture pins down a shape that actually occurs in CT.gov data.
 */
class EligibilityCriteriaChunkerTest {

    private final EligibilityCriteriaChunker chunker = new EligibilityCriteriaChunker();

    private String fixture(String name) {
        try (var in = getClass().getResourceAsStream("/eligibility/" + name)) {
            if (in == null) throw new IllegalStateException("missing fixture: " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    @DisplayName("well-formed trial splits into separate inclusion and exclusion criteria")
    void cleanBaselineSplitsBothSections() {
        List<EligibilityChunk> chunks = chunker.chunk(fixture("clean-baseline-NCT06649565.txt"));

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).anyMatch(c -> c.type() == ChunkType.INCLUSION);
        assertThat(chunks).anyMatch(c -> c.type() == ChunkType.EXCLUSION);
        assertThat(chunks).noneMatch(c -> c.type() == ChunkType.UNPARSED);

        // One chunk per criterion, not one per section.
        assertThat(chunks.stream().filter(c -> c.type() == ChunkType.INCLUSION))
                .as("inclusion criteria should be split individually")
                .hasSizeGreaterThan(1);

        // Section headers must not survive as chunk text.
        assertThat(chunks).noneMatch(c -> c.text().toLowerCase().startsWith("inclusion criteria"));
        assertThat(chunks).noneMatch(c -> c.text().toLowerCase().startsWith("exclusion criteria"));
    }

    @Test
    @DisplayName("escaped markdown is unescaped and headers still match (46% of sampled corpus)")
    void escapedMarkdownIsHandled() {
        String raw = fixture("escaped-markdown-NCT01303679.txt");
        assertThat(raw).as("fixture must actually contain escaping, else it proves nothing")
                .contains("\\");

        List<EligibilityChunk> chunks = chunker.chunk(raw);

        // The real regression: escaping must not push the trial into the unparsed fallback.
        assertThat(chunks).noneMatch(c -> c.type() == ChunkType.UNPARSED);
        assertThat(chunks).anyMatch(c -> c.type() == ChunkType.INCLUSION);
        // No leftover backslash-escapes in the embeddable text.
        assertThat(chunks).noneMatch(c -> c.text().contains("\\*"));
        assertThat(chunks).noneMatch(c -> c.text().contains("\\<"));
    }

    @Test
    @DisplayName("nested sub-criteria are prefixed with their parent line")
    void nestedCriteriaCarryParentContext() {
        List<EligibilityChunk> chunks = chunker.chunk(fixture("nested-numbered-NCT04942054.txt"));

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).noneMatch(c -> c.type() == ChunkType.UNPARSED);

        // Every chunk must stand alone semantically - the point of parent prefixing. A bare
        // fragment like "age >= 60 years" is useless as a retrieval unit.
        assertThat(chunks)
                .as("no chunk should be a bare fragment")
                .allMatch(c -> c.text().length() >= 3);
    }

    @Test
    @DisplayName("header without a colon is still recognised (longest sampled trial)")
    void headerWithoutColonIsRecognised() {
        String raw = fixture("longest-nested-NCT04244552.txt");
        assertThat(raw).as("this fixture's header has no colon").contains("Inclusion Criteria\n");

        List<EligibilityChunk> chunks = chunker.chunk(raw);

        assertThat(chunks).noneMatch(c -> c.type() == ChunkType.UNPARSED);
        assertThat(chunks).anyMatch(c -> c.type() == ChunkType.INCLUSION);
    }

    @Test
    @DisplayName("13,771-char trial is split small enough for the embedding window")
    void longestTrialIsSplitBelowEmbeddingWindow() {
        String raw = fixture("longest-nested-NCT04244552.txt");
        List<EligibilityChunk> chunks = chunker.chunk(raw);

        // The local MiniLM model's window is ~256 word pieces, roughly 1000 chars. Chunks
        // far above that are silently truncated at embed time, so this is the assertion
        // that the whole per-criterion strategy exists to satisfy.
        assertThat(chunks).hasSizeGreaterThan(10);
        assertThat(chunks.stream().mapToInt(c -> c.text().length()).max().orElseThrow())
                .as("largest chunk should be far smaller than the 13771-char source")
                .isLessThan(3000);
    }

    @Test
    @DisplayName("legacy trial with no inclusion/exclusion headers falls back to one unparsed chunk")
    void legacyFormatFallsBackToUnparsed() {
        List<EligibilityChunk> chunks = chunker.chunk(fixture("legacy-caps-NCT00003680.txt"));

        // Older records use "DISEASE CHARACTERISTICS:" / "PATIENT CHARACTERISTICS:" - an
        // inclusion/exclusion split does not exist to be recovered, so admit that rather
        // than guess. Counting these is a quality metric (RAG_PLAN.md section 5).
        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().type()).isEqualTo(ChunkType.UNPARSED);
        assertThat(chunks.getFirst().text()).contains("DISEASE CHARACTERISTICS");
    }

    @Test
    @DisplayName("ordinals restart per section and preserve document order")
    void ordinalsRestartPerSection() {
        List<EligibilityChunk> chunks = chunker.chunk(fixture("clean-baseline-NCT06649565.txt"));

        List<Integer> inclusionOrdinals = chunks.stream()
                .filter(c -> c.type() == ChunkType.INCLUSION)
                .map(EligibilityChunk::ordinal)
                .toList();
        List<Integer> exclusionOrdinals = chunks.stream()
                .filter(c -> c.type() == ChunkType.EXCLUSION)
                .map(EligibilityChunk::ordinal)
                .toList();

        assertThat(inclusionOrdinals).startsWith(0).isSorted();
        assertThat(exclusionOrdinals).startsWith(0).isSorted();
    }

    // ---- Regression tests for defects found by inspecting real output. The original
    // assertions were structural (non-empty, length >= 3) and passed happily on bad chunks;
    // these encode the actual quality rules.

    @Test
    @DisplayName("grouping labels are never emitted as standalone chunks")
    void groupingLabelsAreNotEmitted() {
        List<EligibilityChunk> chunks = chunker.chunk(fixture("nested-numbered-NCT04942054.txt"));

        // "All 3 parts of Study:" is a sub-heading, not a criterion - nothing to retrieve.
        assertThat(chunks)
                .as("a bare label ending in ':' has no retrievable content")
                .noneMatch(c -> c.text().strip().equals("All 3 parts of Study:"));
        assertThat(chunks)
                .allMatch(c -> !c.text().strip().endsWith(":") || c.type() == ChunkType.UNPARSED);
    }

    @Test
    @DisplayName("labels are not prefixed onto their children")
    void labelsAreNotPrefixedOntoChildren() {
        List<EligibilityChunk> chunks = chunker.chunk(fixture("nested-numbered-NCT04942054.txt"));

        // Prefixing "All 3 parts of Study:" onto all 42 chunks gave every chunk the same
        // leading phrase, pulling their embeddings together and blurring the distinctions
        // retrieval depends on.
        assertThat(chunks)
                .as("no chunk should carry the grouping label as a prefix")
                .noneMatch(c -> c.text().startsWith("All 3 parts of Study:"));
    }

    @Test
    @DisplayName("no chunk is fully contained in another, adding nothing of its own")
    void noChunkIsSubsumedByAnother() {
        List<EligibilityChunk> chunks = chunker.chunk(fixture("longest-nested-NCT04244552.txt"));

        // The rule is containment, not prefix-sharing. Siblings legitimately share a parent
        // prefix - that is the point of parent prefixing. What must not happen is a chunk
        // whose entire text is already inside another chunk: it contributes no retrievable
        // information and competes with the richer chunk for the same query.
        for (EligibilityChunk candidate : chunks) {
            boolean subsumed = chunks.stream()
                    .filter(other -> other != candidate)
                    .anyMatch(other -> other.text().contains(candidate.text()));
            assertThat(subsumed)
                    .as("chunk adds nothing over a sibling that contains it: \"%s\"", candidate.text())
                    .isFalse();
        }
    }

    @Test
    @DisplayName("a parent whose children carry the detail is not emitted on its own")
    void parentWithChildrenIsNotEmittedAlone() {
        List<EligibilityChunk> chunks = chunker.chunk(fixture("longest-nested-NCT04244552.txt"));

        // "Absolute neutrophil count (ANC)" has two nested children holding the actual
        // thresholds. Alone it names a lab value with no threshold - the one chunk that
        // cannot answer "what neutrophil count do I need?". Its children are prefixed with
        // it, so the term stays searchable without emitting the bare parent.
        assertThat(chunks)
                .noneMatch(c -> c.text().strip().equals("Absolute neutrophil count (ANC)"));
        assertThat(chunks)
                .as("the threshold-bearing children survive and keep the parent's context")
                .anyMatch(c -> c.text().contains("Absolute neutrophil count (ANC)")
                        && c.text().contains("1000"));
    }

    @Test
    @DisplayName("parent prefixes are abbreviated rather than copied whole")
    void parentPrefixesAreAbbreviated() {
        List<EligibilityChunk> chunks = chunker.chunk(fixture("longest-nested-NCT04244552.txt"));

        // A nested child should carry enough parent context to be interpretable, but the
        // parent's full text (500+ chars here) would dominate the child's own content.
        EligibilityChunk nested = chunks.stream()
                .filter(c -> c.text().contains("BRAF mutant melanoma"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected the nested BRAF criterion"));

        assertThat(nested.text())
                .as("child keeps its qualifying cohort context")
                .contains("pembrolizumab combination therapy cohort");
        assertThat(nested.text().length())
                .as("but the parent is abbreviated, not copied whole")
                .isLessThan(400);
    }

    @Test
    @DisplayName("null and blank input yield no chunks rather than throwing")
    void nullAndBlankAreSafe() {
        assertThat(chunker.chunk(null)).isEmpty();
        assertThat(chunker.chunk("")).isEmpty();
        assertThat(chunker.chunk("   \n  \n ")).isEmpty();
    }

    @Test
    @DisplayName("unicode bullets split criteria instead of merging into the previous one")
    void unicodeBulletsAreSplit() {
        // 205 lines across 107 trials use • rather than *. Unmatched, each one merges into the
        // criterion above it and carries the glyph into the embedded text.
        List<EligibilityChunk> chunks = chunker.chunk("""
                Inclusion criteria:

                • Age 18 years or older
                ▪ Measurable disease per RECIST 1.1
                ● Adequate organ function
                ○ Life expectancy over 12 weeks
                · ECOG performance status 0 or 1
                """);

        assertThat(chunks).hasSize(5);
        assertThat(chunks).allSatisfy(c -> assertThat(c.text())
                .as("the bullet glyph must not survive into the chunk text")
                .doesNotContain("•", "▪", "●", "○", "·"));
        assertThat(chunks.get(0).text()).isEqualTo("Age 18 years or older");
        assertThat(chunks.get(4).text()).isEqualTo("ECOG performance status 0 or 1");
    }

    @Test
    @DisplayName("a unicode bullet with no following space is still a bullet")
    void unicodeBulletWithoutSpace() {
        // 10 of 217 surveyed lines are written •Patients with no gap.
        List<EligibilityChunk> chunks = chunker.chunk("""
                Inclusion criteria:

                •Able to provide informed consent
                •Able to speak English
                """);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).text()).isEqualTo("Able to provide informed consent");
    }

    @Test
    @DisplayName("comparison symbols leading a line are content, not bullet markers")
    void comparisonSymbolsAreNotBullets() {
        // ≥ ≤ < ° lead lines as criterion content. Treating them as markers would strip the
        // symbol and invert the meaning of the criterion.
        List<EligibilityChunk> chunks = chunker.chunk("""
                Inclusion criteria:

                * Absolute neutrophil count
                ≥ 1500 per microliter
                """);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).text())
                .as("the continuation keeps its comparison symbol")
                .contains("≥ 1500");
    }
}
