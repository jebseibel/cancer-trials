package com.seibel.cancer.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunable RAG values, surfaced in application.yml so they can be changed without a rebuild.
 *
 * <p>These were all chosen empirically by inspecting real chunk output and real query results,
 * so they are the values most likely to need adjusting - for a different corpus, or after an
 * embedding-model change. The evaluation set
 * ({@code rag/src/test/resources/eval/run-evaluation.sh}) is what makes tuning them measurable
 * rather than guesswork.
 *
 * <p><b>Chunking values are not hot-reloadable in any useful sense.</b> They only affect chunks
 * as they are created, so changing one requires a full re-backfill
 * ({@code POST /api/rag/backfill}) before it has any effect on search results.
 *
 * <p>Namespaced under {@code cancer.rag} rather than {@code spring.ai} - these are this
 * project's values, not Spring AI's.
 */
@Data
@ConfigurationProperties(prefix = "cancer.rag")
public class RagProperties {

    private Chunking chunking = new Chunking();
    private Retrieval retrieval = new Retrieval();
    private Backfill backfill = new Backfill();
    private Progress progress = new Progress();

    @Data
    public static class Chunking {

        /**
         * Longest a parent criterion may be before it is abbreviated when prefixed onto a
         * nested child.
         *
         * <p>Exists because some parents run 500+ chars; prefixing one whole onto each child
         * made siblings ~90% identical, and near-identical vectors mean one query match crowds
         * out the others. Raise it to keep more parent context, lower it to keep chunks
         * distinct.
         */
        private int maxParentPrefixLength = 160;

        /** Text shorter than this is not worth embedding as its own chunk. */
        private int minCriterionLength = 3;

        /**
         * Target size for prose chunks (brief summary, detailed description).
         *
         * <p>Tuned to the local embedding model's usable window: all-MiniLM-L6-v2 handles about
         * 256 word pieces, roughly 1000 characters, and text past that is silently truncated at
         * embed time. <b>Raise this when moving to a model with a longer window</b> - a 768-dim
         * -base model typically handles 512 tokens.
         */
        private int maxProseChunkChars = 900;
    }

    @Data
    public static class Retrieval {

        /**
         * How many chunks to fetch per requested trial.
         *
         * <p>Several chunks routinely belong to the same trial, so fetching exactly topK chunks
         * yields far fewer than topK distinct trials. Raise it if searches return fewer trials
         * than asked for; lower it to cut work per query.
         */
        private int chunkFetchMultiplier = 6;

        /**
         * Default minimum similarity score when a request does not specify one, 0..1.
         *
         * <p>Left at 0 deliberately: the useful thresholds differ per query type (exact clinical
         * terminology scores ~0.97, conceptual queries ~0.4), and silently dropping results
         * makes weak retrieval look like an empty corpus. Raise it once the corpus and model are
         * settled and you would rather see nothing than see noise.
         */
        private double defaultSimilarityThreshold = 0.0;
    }

    @Data
    public static class Backfill {

        /** Trials per page during backfill. Bounds memory and the embedding batch size. */
        private int pageSize = 25;
    }

    /**
     * Console progress-bar settings for the backfill loop.
     *
     * <p>Separate from {@code cancer.ingestion.progress}, which configures the same
     * {@link com.seibel.cancer.common.progress.ProgressTicker} for the datafetcher's loops.
     * The two modules are independent - {@code :rag} does not depend on {@code :datafetcher} -
     * and the runs have very different shapes, so they get separate knobs rather than a shared
     * one.
     *
     * <p>The bar earns its place here more than anywhere: a full backfill is ~25 minutes of
     * embedding during which the only log output is a single summary line at the very end.
     */
    @Data
    public static class Progress {

        /**
         * {@code true} (default), {@code auto}, or {@code false}.
         *
         * <p>Defaults to {@code true} for the same reason the ingestion bar does: backfill is
         * triggered from the frontend against a running backend, so the work happens on an HTTP
         * request thread with no attached console. {@code auto} resolves to false there and the
         * bar never draws - console detection describes how the process was launched, not
         * whether anyone is watching.
         */
        private String enabled = "true";

        /** Trials per line, before wrapping to a new gutter. Match it to your terminal width. */
        private int lineWidth = 100;

        /** Flush stdout every N trials. {@code System.out} is line-buffered; see the ticker. */
        private int flushInterval = 5;

        /**
         * Resolves {@link #enabled} against the current runtime.
         *
         * @param consoleAttached whether the JVM has an attached console
         */
        public boolean resolveEnabled(boolean consoleAttached) {
            if (enabled == null || enabled.isBlank() || "auto".equalsIgnoreCase(enabled.trim())) {
                return consoleAttached;
            }
            return Boolean.parseBoolean(enabled.trim());
        }
    }
}
