package com.seibel.cancer.rag.chunk;

import com.seibel.cancer.common.domain.Intervention;
import com.seibel.cancer.common.domain.Outcome;
import com.seibel.cancer.common.domain.Trial;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers the trial-level assembly: all chunk types present, metadata correct, ids stable.
 * Eligibility parsing itself is covered by {@link EligibilityCriteriaChunkerTest}.
 */
class TrialChunkerTest {

    private final TrialChunker chunker = new TrialChunker();

    private String fixture(String name) {
        try (var in = getClass().getResourceAsStream("/eligibility/" + name)) {
            if (in == null) throw new IllegalStateException("missing fixture: " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Trial trial() {
        Trial t = new Trial();
        t.setId(42L);
        t.setExtid("trial-extid-abc");
        t.setNctId("NCT06649565");
        t.setOverallStatus("RECRUITING");
        t.setStudyType("INTERVENTIONAL");
        t.setBriefSummary("A study of neoadjuvant therapy response in invasive breast cancer.");
        t.setDetailedDescription("Participants receive standard therapy followed by imaging assessment.");
        t.setEligibilityCriteria(fixture("clean-baseline-NCT06649565.txt"));
        return t;
    }

    private Intervention intervention(String type, String name) {
        Intervention i = new Intervention();
        i.setType(type);
        i.setName(name);
        return i;
    }

    private Outcome outcome(String type, String measure) {
        Outcome o = new Outcome();
        o.setOutcomeType(type);
        o.setMeasure(measure);
        return o;
    }

    @Test
    @DisplayName("produces every chunk type from a trial and its children")
    void producesAllChunkTypes() {
        List<TrialChunk> chunks = chunker.chunk(trial(),
                List.of(intervention("DRUG", "Pembrolizumab")),
                List.of(outcome("PRIMARY", "Pathological complete response rate")));

        List<TrialChunk.Source> sources = chunks.stream().map(TrialChunk::source).distinct().toList();
        assertThat(sources).contains(
                TrialChunk.Source.INCLUSION_CRITERION,
                TrialChunk.Source.EXCLUSION_CRITERION,
                TrialChunk.Source.BRIEF_SUMMARY,
                TrialChunk.Source.DETAILED_DESCRIPTION,
                TrialChunk.Source.INTERVENTION,
                TrialChunk.Source.OUTCOME);
    }

    @Test
    @DisplayName("every chunk carries the trial extid, never the numeric id")
    void metadataUsesExtidOnly() {
        List<TrialChunk> chunks = chunker.chunk(trial(), List.of(), List.of());

        assertThat(chunks).isNotEmpty();
        assertThat(chunks).allMatch(c -> "trial-extid-abc".equals(c.trialExtid()));
        // The project's extid-only rule applies to the vector store too - an internal id must
        // not leak here any more than it reaches the frontend.
        assertThat(chunks).allSatisfy(c ->
                assertThat(c.metadata()).doesNotContainKey("trialId").doesNotContainKey("id"));
        assertThat(chunks).allSatisfy(c ->
                assertThat(c.metadata().get("trialExtid")).isEqualTo("trial-extid-abc"));
    }

    @Test
    @DisplayName("exclusion criteria are flagged so retrieval can tell them from qualifying matches")
    void exclusionCriteriaAreFlagged() {
        List<TrialChunk> chunks = chunker.chunk(trial(), List.of(), List.of());

        TrialChunk exclusion = chunks.stream()
                .filter(c -> c.source() == TrialChunk.Source.EXCLUSION_CRITERION)
                .findFirst().orElseThrow();
        TrialChunk inclusion = chunks.stream()
                .filter(c -> c.source() == TrialChunk.Source.INCLUSION_CRITERION)
                .findFirst().orElseThrow();

        // Matching an exclusion means the opposite of qualifying, so the distinction has to
        // survive into filterable metadata.
        assertThat(exclusion.metadata().get("isExclusion")).isEqualTo(true);
        assertThat(inclusion.metadata().get("isExclusion")).isEqualTo(false);
    }

    @Test
    @DisplayName("chunk ids are deterministic so re-indexing overwrites instead of duplicating")
    void chunkIdsAreDeterministic() {
        List<TrialChunk> first = chunker.chunk(trial(), List.of(), List.of());
        List<TrialChunk> second = chunker.chunk(trial(), List.of(), List.of());

        assertThat(first.stream().map(TrialChunk::id).toList())
                .isEqualTo(second.stream().map(TrialChunk::id).toList());
        // Ids must also be unique within a trial, or chunks overwrite each other.
        assertThat(first.stream().map(TrialChunk::id).distinct().count())
                .isEqualTo(first.size());
    }

    @Test
    @DisplayName("chunk ids are valid UUIDs, which Qdrant requires for point ids")
    void chunkIdsAreValidUuids() {
        List<TrialChunk> chunks = chunker.chunk(trial(), List.of(), List.of());

        // Regression: ids were originally the readable "extid:SOURCE:ordinal" key, and Qdrant
        // rejected every write with "UUID string too large" - point ids must be a UUID or an
        // unsigned integer. The readable key is now hashed into a UUID and kept in metadata.
        assertThat(chunks).isNotEmpty();
        assertThat(chunks).allSatisfy(c ->
                assertThat(java.util.UUID.fromString(c.id())).isNotNull());
        assertThat(chunks).allSatisfy(c ->
                assertThat(c.metadata().get("naturalKey")).isEqualTo(c.naturalKey()));
    }

    @Test
    @DisplayName("status and nctId are carried for filtering and citation")
    void carriesFilterableMetadata() {
        TrialChunk c = chunker.chunk(trial(), List.of(), List.of()).getFirst();

        assertThat(c.metadata().get("overallStatus")).isEqualTo("RECRUITING");
        assertThat(c.metadata().get("studyType")).isEqualTo("INTERVENTIONAL");
        // nctId is what a citation links back to on clinicaltrials.gov.
        assertThat(c.metadata().get("nctId")).isEqualTo("NCT06649565");
    }

    @Test
    @DisplayName("no chunk exceeds the local embedding model's usable window")
    void chunksFitEmbeddingWindow() {
        Trial t = trial();
        t.setEligibilityCriteria(fixture("longest-nested-NCT04244552.txt"));

        List<TrialChunk> chunks = chunker.chunk(t, List.of(), List.of());

        // MiniLM's window is ~256 word pieces, roughly 1000 chars. Text past that is
        // silently truncated at embed time, so oversized chunks are invisible content.
        assertThat(chunks.stream().mapToInt(c -> c.text().length()).max().orElseThrow())
                .isLessThan(3000);
    }

    @Test
    @DisplayName("a trial with no extid yields no chunks rather than unattributable ones")
    void trialWithoutExtidYieldsNothing() {
        Trial t = trial();
        t.setExtid(null);
        assertThat(chunker.chunk(t, List.of(), List.of())).isEmpty();
        assertThat(chunker.chunk(null, List.of(), List.of())).isEmpty();
    }
}
