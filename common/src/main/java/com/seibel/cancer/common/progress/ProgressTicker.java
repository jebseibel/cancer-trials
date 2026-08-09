package com.seibel.cancer.common.progress;

/**
 * Emits a character-per-record progress bar to stdout during a long-running import.
 *
 * <pre>
 *      1 | ****.**!***.*********!**********.***************
 *    101 | ************************!!!!!!!!!***************
 * </pre>
 *
 * A clump of {@code !} at record 148 says something a final summary line never will.
 * This reports <em>where you are</em>, live; the summary reports what happened after.
 *
 * <p>Not thread-safe. One instance per import loop.
 *
 * <p>Implements {@link AutoCloseable} so try-with-resources terminates a partial final
 * line even when the loop exits by exception:
 *
 * <pre>
 * try (ProgressTicker ticker = new ProgressTicker("NORMALIZING")) {
 *     for (Row row : rows) {
 *         ...
 *         ticker.tick();
 *     }
 * }
 * </pre>
 */
public class ProgressTicker implements AutoCloseable {

    private static final int DEFAULT_LINE_WIDTH = 100;
    private static final int DEFAULT_FLUSH_INTERVAL = 10;

    private final int lineWidth;
    private final int flushInterval;
    private final boolean enabled;

    /** Expected record count, for ETA. Zero means unknown - elapsed is still shown. */
    private final int total;

    /**
     * Wall-clock start. Deliberately taken at construction rather than on the first tick:
     * the gap between them is setup the user is also waiting through.
     */
    private final long startedAtNanos = System.nanoTime();

    private int count = 0;
    private int successes = 0;
    private int skips = 0;
    private int errors = 0;

    /**
     * Auto-disables when there is no attached console, so a redirected log or a CI run
     * doesn't collect 2,000 asterisks.
     */
    public ProgressTicker(String label) {
        this(label, DEFAULT_LINE_WIDTH, DEFAULT_FLUSH_INTERVAL, System.console() != null);
    }

    public ProgressTicker(String label, int lineWidth, boolean enabled) {
        this(label, lineWidth, DEFAULT_FLUSH_INTERVAL, enabled);
    }

    public ProgressTicker(String label, int lineWidth, int flushInterval, boolean enabled) {
        this(label, lineWidth, flushInterval, enabled, 0);
    }

    /**
     * @param lineWidth     records per line before wrapping; values below 1 fall back to the
     *                      default, since a non-positive width would break the gutter modulo
     * @param flushInterval flush stdout every N records; values below 1 are clamped to 1
     * @param total         expected record count, enabling an ETA. Pass 0 when unknown - the
     *                      bar then reports elapsed and rate without projecting a finish time
     */
    public ProgressTicker(String label, int lineWidth, int flushInterval, boolean enabled, int total) {
        this.lineWidth = lineWidth > 0 ? lineWidth : DEFAULT_LINE_WIDTH;
        this.flushInterval = Math.max(1, flushInterval);
        this.enabled = enabled;
        this.total = Math.max(0, total);
        if (enabled && label != null && !label.isBlank()) {
            System.out.println();
            System.out.println(label);
            System.out.flush();
        }
    }

    /** Record inserted. */
    public void tick() {
        successes++;
        mark('*');
    }

    /** Record skipped by a filter. */
    public void skip() {
        skips++;
        mark('.');
    }

    /** Record failed. */
    public void error() {
        errors++;
        mark('!');
    }

    private void mark(char glyph) {
        if (!enabled) {
            count++;              // still count, so the getters stay accurate
            return;
        }

        if (count % lineWidth == 0) {
            System.out.printf("%6d | ", count + 1);   // leading gutter, line's first record
        }

        System.out.print(glyph);
        count++;

        if (count % lineWidth == 0) {
            System.out.println(timings());
            System.out.flush();
        } else if (count % flushInterval == 0) {
            // System.out is line-buffered: without this, a whole line of glyphs appears at
            // once when it wraps, which defeats the point. Flushing every record is a real
            // syscall cost on a large import; every 10 is the compromise. Configurable via
            // cancer.ingestion.progress.flush-interval, and clamped to >= 1 in the constructor.
            System.out.flush();
        }
    }

    /**
     * Trailing " | 1m14s 3.4/s ETA 2m01s", appended when a line wraps and to the final line.
     *
     * <p>The ETA is a flat average over the whole run so far, not a windowed rate. That is
     * honest for these import loops, where per-record cost is roughly steady; it would mislead
     * on a loop whose work varies wildly record to record.
     */
    private String timings() {
        long elapsedNanos = System.nanoTime() - startedAtNanos;
        StringBuilder out = new StringBuilder("  ").append(formatDuration(elapsedNanos / 1_000_000L));

        // Rate is computed in nanos, not millis: a fast loop can finish several records inside
        // a single millisecond, and dividing by a zero millisecond count would drop the rate
        // and ETA exactly when the bar scrolls fastest.
        if (elapsedNanos > 0 && count > 0) {
            out.append(String.format(" %.1f/s", count * 1_000_000_000.0 / elapsedNanos));

            // Only project a finish time when the caller told us how many records to expect
            // and there are still some left.
            if (total > count) {
                long remainingNanos = (long) ((total - count) * (elapsedNanos / (double) count));
                out.append(" ETA ").append(formatDuration(remainingNanos / 1_000_000L));
            }
        }
        return out.toString();
    }

    /** Compact and scannable: "4.2s", "45s", "2m01s", "1h04m". */
    private static String formatDuration(long millis) {
        long totalSeconds = millis / 1000L;
        if (totalSeconds < 10) {
            // One decimal under ten seconds: whole-second flooring renders every short
            // duration as "0s", which makes a fast run's ETA look broken rather than quick.
            return String.format("%.1fs", millis / 1000.0);
        }
        if (totalSeconds < 60) {
            return totalSeconds + "s";
        }
        if (totalSeconds < 3600) {
            return String.format("%dm%02ds", totalSeconds / 60, totalSeconds % 60);
        }
        return String.format("%dh%02dm", totalSeconds / 3600, (totalSeconds % 3600) / 60);
    }

    /**
     * Terminates a partial line. Always call this when the loop ends, including on the
     * exception path - prefer try-with-resources, which calls it for you.
     */
    public void finish() {
        if (!enabled) {
            return;
        }
        if (count % lineWidth != 0) {
            // Pad the partial line so the timing column stays aligned with the full lines above.
            System.out.print(" ".repeat(lineWidth - (count % lineWidth)));
            System.out.println(timings());
        } else if (count > 0) {
            // Exact-width run: the last line already wrapped and carries its own timings, so
            // just report the total rather than emitting a blank line.
            System.out.println("  total " + formatDuration(
                    (System.nanoTime() - startedAtNanos) / 1_000_000L));
        }
        System.out.flush();
    }

    @Override
    public void close() {
        finish();
    }

    public int getCount()     { return count; }
    public int getSuccesses() { return successes; }
    public int getSkips()     { return skips; }
    public int getErrors()    { return errors; }
}
