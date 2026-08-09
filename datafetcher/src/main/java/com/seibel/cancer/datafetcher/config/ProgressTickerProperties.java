package com.seibel.cancer.datafetcher.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Console progress-bar settings for the ingestion loops.
 *
 * <p>Lives here rather than beside {@code ProgressTicker} in {@code :common} because
 * {@code :common} is deliberately framework-light - it has no Spring dependency at all. The
 * ticker stays a plain object; this class is what turns YAML into its constructor arguments.
 *
 * @see com.seibel.cancer.common.progress.ProgressTicker
 */
@Data
@ConfigurationProperties(prefix = "cancer.ingestion.progress")
public class ProgressTickerProperties {

    /**
     * {@code true} (default), {@code auto}, or {@code false}.
     *
     * <p>Defaults to {@code true} because ingestion is triggered from the frontend against an
     * already-running backend: that request thread has no attached console, so {@code auto}
     * resolves to false and the bar never draws.
     *
     * <p>{@code auto} keeps the console check, for a launch that actually has one - it avoids
     * collecting thousands of asterisks in a captured log. {@code false} disables the bar; the
     * counters stay accurate either way.
     */
    private String enabled = "true";

    /** Records per line, before wrapping to a new gutter. Match it to your terminal width. */
    private int lineWidth = 100;

    /**
     * Flush stdout every N records.
     *
     * <p>{@code System.out} is line-buffered, so without a periodic flush a whole line of glyphs
     * appears at once when it wraps - which defeats the purpose of a live bar. Flushing every
     * single record is a real syscall cost on a large import; 10 is the compromise. Lower it for
     * a smoother bar, raise it for less overhead. Values below 1 are treated as 1.
     */
    private int flushInterval = 10;

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
