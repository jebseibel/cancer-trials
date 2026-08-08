package com.seibel.cancer.rag.chunk;

import com.seibel.cancer.rag.chunk.EligibilityChunk.ChunkType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits a trial's raw eligibility_criteria text into one chunk per criterion.
 *
 * <p>Per-criterion rather than fixed-size windows, for two reasons: a query like "no prior
 * chemotherapy" should match one specific criterion line instead of a wall of text, and
 * citation back to the exact matched line is only possible if the chunk *is* that line.
 * The local embedding model's ~256 word-piece window reinforces it - the longest criteria
 * block observed was 13,771 chars, roughly 14x the window, so coarse chunks would be
 * silently truncated and unsearchable.
 *
 * <p>Every rule here was derived from 50 real trials pulled through this project's own
 * ingestion pipeline, not from assumptions. See
 * {@code rag/src/test/resources/eligibility/README.md} for the measured frequencies.
 * Notably the hand-built CT.gov fixture in :datafetcher uses {@code - } hyphen bullets and
 * a tidy "Inclusion Criteria:" header - <em>neither shape occurs in real data</em>.
 *
 * <p>No Spring dependency: plain text in, chunks out, so the hard logic stays fast to test.
 */
public class EligibilityCriteriaChunker {

    /**
     * Section headers. Colon is optional - the longest trial observed
     * (NCT04244552, 13,771 chars) writes "Inclusion Criteria" with no colon. Leading
     * markdown emphasis is already removed by unescaping + marker stripping before this
     * runs. Case-insensitive because casing varies by sponsor.
     */
    private static final Pattern SECTION_HEADER =
            Pattern.compile("^\\s*(inclusion|exclusion)\\s+criteria\\s*:?\\s*$",
                    Pattern.CASE_INSENSITIVE);

    /**
     * A bullet line, capturing indent depth and content.
     *
     * <p>Deliberately does NOT match {@code - } hyphens: across the 50-trial sample there
     * were 632 asterisk-bullet lines and 163 numbered, and <em>zero</em> hyphen bullets.
     * Hyphens do appear mid-sentence in clinical text ("HER2-positive", "day 1-21"), so
     * matching them would split criteria at the wrong places.
     */
    private static final Pattern BULLET =
            Pattern.compile("^(\\s*)(?:\\*|\\d+\\.|[a-z]\\))\\s+(.*)$");

    /** Defaults, used by the no-arg constructor. Overridden from {@code cancer.rag.chunking.*}. */
    private static final int DEFAULT_MAX_PARENT_PREFIX_LENGTH = 160;
    private static final int DEFAULT_MIN_CRITERION_LENGTH = 3;

    /** Longest a parent prefix may be before it is abbreviated. See abbreviateParent(). */
    private final int maxParentPrefixLength;

    /** Text a criterion must exceed to be worth embedding on its own. */
    private final int minCriterionLength;

    public EligibilityCriteriaChunker() {
        this(DEFAULT_MAX_PARENT_PREFIX_LENGTH, DEFAULT_MIN_CRITERION_LENGTH);
    }

    public EligibilityCriteriaChunker(int maxParentPrefixLength, int minCriterionLength) {
        this.maxParentPrefixLength = maxParentPrefixLength;
        this.minCriterionLength = minCriterionLength;
    }

    /**
     * A bullet that is a grouping label rather than a criterion - "All 3 parts of Study:",
     * "Confirmed diagnosis of:". Ends in a colon and carries no sentence of its own.
     *
     * <p>These are structurally parents but semantically empty, and treating them as
     * context actively hurt retrieval: prefixing "All 3 parts of Study:" onto all 42 chunks
     * of a trial gave every chunk the same leading phrase, pulling their embeddings
     * together and blurring the distinctions search depends on. They are used for grouping
     * and then dropped.
     */
    private static final Pattern SECTION_LABEL = Pattern.compile("^[^.!?]{0,60}:$");


    public List<EligibilityChunk> chunk(String rawCriteria) {
        if (rawCriteria == null || rawCriteria.isBlank()) {
            return List.of();
        }

        List<String> lines = normalize(rawCriteria).lines().toList();

        return hasAnySectionHeader(lines)
                ? chunkBySection(lines)
                : List.of(unparsedFallback(rawCriteria));
    }

    /**
     * Undo the markdown escaping that reaches the database verbatim - 23 of 50 sampled
     * trials contained {@code \*} or {@code \<}. Left in place, "\*\*Inclusion
     * Criteria:\*\*" never matches the header pattern and 46% of the corpus silently falls
     * through to the unparsed fallback.
     */
    private String normalize(String raw) {
        String text = raw.replace("\r\n", "\n").replace('\r', '\n');
        // Unescape backslash-escaped markdown punctuation.
        text = text.replaceAll("\\\\([*_<>#\\[\\]()`~])", "$1");
        // Strip paired emphasis markers around header text ("**Inclusion Criteria:**").
        text = text.replaceAll("(?m)^\\s*\\*{2,}\\s*(.*?)\\s*\\*{2,}\\s*$", "$1");
        return text;
    }

    private boolean hasAnySectionHeader(List<String> lines) {
        return lines.stream().anyMatch(l -> SECTION_HEADER.matcher(l).matches());
    }

    private EligibilityChunk unparsedFallback(String rawCriteria) {
        // Whole block, one chunk. No inclusion/exclusion headers were found, and guessing
        // would be worse than admitting it - these are mostly older records using the
        // legacy "DISEASE CHARACTERISTICS:" / "PATIENT CHARACTERISTICS:" convention, where
        // an inclusion/exclusion split does not exist to be recovered.
        return new EligibilityChunk(collapseWhitespace(normalize(rawCriteria)), ChunkType.UNPARSED, 0);
    }

    private List<EligibilityChunk> chunkBySection(List<String> lines) {
        List<EligibilityChunk> chunks = new ArrayList<>();

        ChunkType currentType = null;
        int ordinal = 0;
        // Deepest-indent-first stack of ancestor bullet text, for parent prefixing.
        List<Indented> ancestors = new ArrayList<>();
        StringBuilder pending = new StringBuilder();
        List<String> pendingAncestors = List.of();
        /** Indent column of the pending bullet, so a deeper following bullet is its child. */
        int pendingIndent = 0;

        for (String line : lines) {
            Matcher header = SECTION_HEADER.matcher(line);
            if (header.matches()) {
                flush(chunks, pending, currentType, ordinal, pendingAncestors);
                if (!pending.isEmpty()) ordinal++;
                pending.setLength(0);
                currentType = header.group(1).equalsIgnoreCase("inclusion")
                        ? ChunkType.INCLUSION : ChunkType.EXCLUSION;
                ordinal = 0;
                ancestors.clear();
                pendingIndent = 0;
                continue;
            }

            if (currentType == null) {
                // Preamble before the first header - not attributable to a section, skip.
                continue;
            }

            Matcher bullet = BULLET.matcher(line);
            if (bullet.matches()) {
                int indent = bullet.group(1).length();
                String content = bullet.group(2).strip();

                // A bullet indented deeper than the pending one is its CHILD, which means
                // the pending bullet is structure rather than a criterion: it is about to be
                // prefixed onto this child, so emitting it alone as well would add a chunk
                // that answers nothing. "Absolute neutrophil count (ANC)" is the canonical
                // case - the thresholds live in its children, so the bare parent is the one
                // chunk that cannot answer "what neutrophil count do I need?".
                boolean pendingIsParentOfThis = !pending.isEmpty() && indent > pendingIndent;

                if (!pendingIsParentOfThis) {
                    if (flush(chunks, pending, currentType, ordinal, pendingAncestors)) ordinal++;
                }
                pending.setLength(0);

                // Pop ancestors at or deeper than this indent - they are siblings, not parents.
                ancestors.removeIf(a -> a.indent() >= indent);
                // Only contentful ancestors become prefixes. Bare labels ("Confirmed
                // diagnosis of:") are kept on the stack for structure but contribute no text.
                pendingAncestors = ancestors.stream()
                        .filter(anc -> !anc.label())
                        .map(Indented::text)
                        .toList();
                pending.append(content);
                pendingIndent = indent;

                // Record as a potential parent for whatever follows.
                ancestors.add(new Indented(indent, content, isSectionLabel(content)));
            } else if (!line.isBlank()) {
                // Continuation of the current bullet's text (wrapped line), or loose prose
                // inside a section with no bullet marker.
                if (pending.isEmpty()) {
                    pendingAncestors = ancestors.stream()
                            .filter(anc -> !anc.label())
                            .map(Indented::text)
                            .toList();
                }
                if (!pending.isEmpty()) pending.append(' ');
                pending.append(line.strip());
            }
        }
        if (flush(chunks, pending, currentType, ordinal, pendingAncestors)) {
            // final chunk emitted
        }
        return chunks;
    }

    /**
     * Emits one chunk if there is pending text worth keeping. Nested items are prefixed
     * with their ancestor lines so the chunk stands alone semantically - the whole reason
     * for tracking ancestors at all.
     *
     * @return true if a chunk was emitted
     */
    private boolean flush(List<EligibilityChunk> chunks, StringBuilder pending,
                          ChunkType type, int ordinal, List<String> ancestors) {
        if (type == null) return false;
        String text = collapseWhitespace(pending.toString());
        if (text.length() < minCriterionLength) return false;

        // A grouping label on its own has nothing to retrieve - "All 3 parts of Study:" as
        // a standalone chunk is a wasted embedding and noise in results. Its children carry
        // the actual content.
        if (isSectionLabel(text)) return false;

        String full = ancestors.isEmpty()
                ? text
                : ancestors.stream().map(this::abbreviateParent)
                        .reduce("", (a, b) -> a.isEmpty() ? b : a + " " + b) + " " + text;

        chunks.add(new EligibilityChunk(collapseWhitespace(full), type, ordinal));
        return true;
    }

    /** True if the text is a grouping label rather than a criterion worth embedding. */
    private boolean isSectionLabel(String text) {
        return SECTION_LABEL.matcher(text.strip()).matches();
    }

    /**
     * Trims a long parent down to just enough context for the child to be interpretable.
     *
     * <p>Some parents run 500+ characters. Prefixing one whole onto each of its children
     * produces chunks that are ~90% identical text, and near-identical embeddings mean a
     * query matches one and crowds out its siblings - the opposite of what per-criterion
     * chunking is for. The first sentence (or clause) carries the qualifying context;
     * the rest is detail the child does not need.
     */
    private String abbreviateParent(String parent) {
        String p = parent.strip();
        if (p.length() <= maxParentPrefixLength) return p;

        // Prefer a clean break at the first sentence or clause boundary.
        int cut = -1;
        for (String delim : List.of(". ", "; ", ": ")) {
            int i = p.indexOf(delim);
            if (i > 0 && i <= maxParentPrefixLength && i > cut) cut = i + 1;
        }
        if (cut > 0) return p.substring(0, cut).strip();

        // No boundary found - hard-trim at a word edge.
        int space = p.lastIndexOf(' ', maxParentPrefixLength);
        return (space > 0 ? p.substring(0, space) : p.substring(0, maxParentPrefixLength)).strip() + "...";
    }

    private String collapseWhitespace(String s) {
        return s.replaceAll("[ \\t]+", " ").strip();
    }

    /**
     * A bullet's text with the indent column it appeared at, and whether it is a grouping
     * label. Labels stay on the ancestor stack so nesting depth is tracked correctly, but
     * are excluded from the prefixes applied to their children.
     */
    private record Indented(int indent, String text, boolean label) {
    }
}
