package com.seibel.cancer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunable values for the root module's matching/AI services, surfaced in application.yml.
 *
 * <p>Namespaced under {@code cancer.matching} to sit alongside {@code cancer.rag} and
 * {@code cancer.ingestion} - each module gets its own key rather than one shared block, since
 * their settings do not overlap.
 */
@Data
@ConfigurationProperties(prefix = "cancer.matching")
public class MatchingProperties {

    private Progress progress = new Progress();

    /**
     * Console progress-bar settings for the friendly-title backfill loop.
     *
     * <p>Separate from {@code cancer.ingestion.progress} and {@code cancer.rag.progress}, which
     * configure the same {@link com.seibel.cancer.common.progress.ProgressTicker} for the
     * datafetcher and rag modules' loops. Each gets its own knobs rather than a shared one,
     * since the runs have different shapes and per-record cost.
     *
     * <p>The bar earns its place here for the same reason as the other two: a corpus-wide run
     * is one paid AI call per trial with nothing printed until a single summary line at the
     * end, which is indistinguishable from a hang.
     */
    @Data
    public static class Progress {

        /**
         * {@code true} (default), {@code auto}, or {@code false}.
         *
         * <p>Defaults to {@code true} for the same reason the ingestion and rag bars do: the
         * backfill is triggered from the frontend against a running backend, so the work
         * happens on an HTTP request thread with no attached console. {@code auto} resolves to
         * false there and the bar never draws - console detection describes how the process was
         * launched, not whether anyone is watching.
         */
        private String enabled = "true";

        /** Trials per line, before wrapping to a new gutter. Match it to your terminal width. */
        private int lineWidth = 100;

        /** Flush stdout every N trials. {@code System.out} is line-buffered; see the ticker. */
        private int flushInterval = 1;

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
