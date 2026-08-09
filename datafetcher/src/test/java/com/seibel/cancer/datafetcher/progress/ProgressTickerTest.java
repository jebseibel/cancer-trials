package com.seibel.cancer.datafetcher.progress;

import com.seibel.cancer.common.progress.ProgressTicker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lives in :datafetcher rather than :common because :common has no test source tree or
 * test dependencies, and :datafetcher is where the ticker is consumed.
 */
class ProgressTickerTest {

    private ByteArrayOutputStream captured;
    private PrintStream originalOut;

    @BeforeEach
    void redirectStdout() {
        captured = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
    }

    @AfterEach
    void restoreStdout() {
        System.setOut(originalOut);
    }

    private String output() {
        return captured.toString(StandardCharsets.UTF_8);
    }

    @Test
    void counters_shouldTrackEachOutcomeSeparately() {
        ProgressTicker ticker = new ProgressTicker(null, 100, true);

        ticker.tick();
        ticker.tick();
        ticker.skip();
        ticker.error();
        ticker.finish();

        assertEquals(4, ticker.getCount());
        assertEquals(2, ticker.getSuccesses());
        assertEquals(1, ticker.getSkips());
        assertEquals(1, ticker.getErrors());
    }

    @Test
    void glyphs_shouldDifferPerOutcome() {
        ProgressTicker ticker = new ProgressTicker(null, 100, true);

        ticker.tick();
        ticker.skip();
        ticker.error();
        ticker.finish();

        assertTrue(output().contains("*.!"), "expected glyph sequence '*.!' in: " + output());
    }

    @Test
    void gutter_shouldShowStartingRecordNumberOfEachLine() {
        ProgressTicker ticker = new ProgressTicker(null, 10, true);

        for (int i = 0; i < 25; i++) {
            ticker.tick();
        }
        ticker.finish();

        // stripTrailing only - a leading strip would eat the first line's gutter padding.
        String[] lines = output().stripTrailing().split("\n");
        assertEquals(3, lines.length);
        // The gutter is the line's FIRST record, so you locate a glyph by counting across.
        // startsWith, not equals: a timing suffix trails each wrapped line.
        assertTrue(lines[0].startsWith("     1 | **********"), "first line: " + lines[0]);
        assertTrue(lines[1].startsWith("    11 | "), "second line gutter: " + lines[1]);
        assertTrue(lines[2].startsWith("    21 | "), "third line gutter: " + lines[2]);
        // Final partial line carries only the 5 remaining glyphs, then padding and timings.
        assertTrue(lines[2].startsWith("    21 | *****"), "third line: " + lines[2]);
        assertFalse(lines[2].contains("******"), "third line should hold 5 glyphs: " + lines[2]);
    }

    @Test
    void finish_shouldTerminateAPartialLineOnly() {
        ProgressTicker partial = new ProgressTicker(null, 10, true);
        for (int i = 0; i < 13; i++) {
            partial.tick();
        }
        partial.finish();
        assertTrue(output().endsWith("\n"), "partial line should be newline-terminated");

        // A run landing exactly on the line width already wrapped; finish() must not add a
        // second newline and leave a blank line behind.
        captured.reset();
        ProgressTicker exact = new ProgressTicker(null, 10, true);
        for (int i = 0; i < 10; i++) {
            exact.tick();
        }
        exact.finish();
        assertFalse(output().endsWith("\n\n"), "exact-width run should not double-newline");
    }

    @Test
    void close_shouldFinishThePartialLineOnTheExceptionPath() {
        assertThrows(IllegalStateException.class, () -> {
            try (ProgressTicker ticker = new ProgressTicker(null, 10, true)) {
                ticker.tick();
                ticker.tick();
                throw new IllegalStateException("loop blew up");
            }
        });

        assertTrue(output().endsWith("\n"), "try-with-resources should terminate the line");
    }

    @Test
    void disabled_shouldPrintNothingButStillCount() {
        ProgressTicker ticker = new ProgressTicker("QUIET", 100, false);

        ticker.tick();
        ticker.skip();
        ticker.error();
        ticker.finish();

        assertEquals("", output(), "disabled ticker must be silent");
        assertEquals(3, ticker.getCount());
        assertEquals(1, ticker.getSuccesses());
        assertEquals(1, ticker.getSkips());
        assertEquals(1, ticker.getErrors());
    }

    @Test
    void flushInterval_shouldClampToOneRatherThanDivideByZero() {
        // flush-interval is configurable now, so a 0 in application.yml must not reach
        // the count % flushInterval modulo.
        ProgressTicker ticker = new ProgressTicker(null, 10, 0, true);

        assertDoesNotThrow(() -> {
            for (int i = 0; i < 5; i++) {
                ticker.tick();
            }
            ticker.finish();
        });
        assertEquals(5, ticker.getCount());
    }

    @Test
    void lineWidth_shouldFallBackToDefaultWhenNonPositive() {
        // A zero width would break the gutter modulo the same way.
        ProgressTicker ticker = new ProgressTicker(null, 0, 10, true);

        assertDoesNotThrow(() -> {
            ticker.tick();
            ticker.finish();
        });
        assertTrue(output().contains("     1 | *"), "expected default-width gutter in: " + output());
    }

    @Test
    void wrappedLine_shouldCarryElapsedAndRate() {
        ProgressTicker ticker = new ProgressTicker(null, 5, 10, true, 0);
        for (int i = 0; i < 5; i++) {
            ticker.tick();
        }
        ticker.finish();

        String firstLine = output().stripTrailing().split("\n")[0];
        assertTrue(firstLine.matches(".*\\*{5}\\s+[\\d.]+s.*"), "expected elapsed on: " + firstLine);
        assertTrue(firstLine.contains("/s"), "expected a rate on: " + firstLine);
    }

    @Test
    void eta_shouldAppearOnlyWhenATotalIsKnownAndRecordsRemain() {
        ProgressTicker withTotal = new ProgressTicker(null, 5, 10, true, 100);
        for (int i = 0; i < 5; i++) {
            withTotal.tick();
        }
        withTotal.finish();
        assertTrue(output().contains("ETA"), "expected an ETA with a known total: " + output());

        // No total supplied - elapsed and rate still shown, but nothing is projected.
        captured.reset();
        ProgressTicker noTotal = new ProgressTicker(null, 5, 10, true, 0);
        for (int i = 0; i < 5; i++) {
            noTotal.tick();
        }
        noTotal.finish();
        assertFalse(output().contains("ETA"), "must not project without a total: " + output());
    }

    @Test
    void eta_shouldDisappearOnceEveryRecordIsAccountedFor() {
        ProgressTicker ticker = new ProgressTicker(null, 5, 10, true, 5);
        for (int i = 0; i < 5; i++) {
            ticker.tick();
        }
        ticker.finish();

        assertFalse(output().contains("ETA"), "no ETA when count has reached total: " + output());
    }

    @Test
    void finalPartialLine_shouldBePaddedSoTimingsStayAligned() {
        ProgressTicker ticker = new ProgressTicker(null, 10, 10, true, 0);
        for (int i = 0; i < 13; i++) {
            ticker.tick();
        }
        ticker.finish();

        String[] lines = output().stripTrailing().split("\n");
        // Locate where the timing suffix starts on each line, rather than matching a literal
        // duration - the format varies with how fast the test happens to run.
        int fullLineTimingColumn = lines[0].indexOf("s ", lines[0].indexOf('|'));
        int partialLineTimingColumn = lines[1].indexOf("s ", lines[1].indexOf('|'));
        assertTrue(fullLineTimingColumn > 0, "no timing on the full line: " + lines[0]);
        assertEquals(fullLineTimingColumn, partialLineTimingColumn,
                "timing column should align across lines:\n" + lines[0] + "\n" + lines[1]);
    }

    @Test
    void exactWidthRun_shouldReportATotalWithoutABlankLine() {
        ProgressTicker ticker = new ProgressTicker(null, 5, 10, true, 0);
        for (int i = 0; i < 5; i++) {
            ticker.tick();
        }
        ticker.finish();

        assertTrue(output().contains("total"), "expected a total line: " + output());
        assertFalse(output().contains("\n\n"), "should not emit a blank line: " + output());
    }

    @Test
    void label_shouldPrintOnceWhenEnabled() {
        ProgressTicker ticker = new ProgressTicker("NORMALIZING", 100, true);
        ticker.tick();
        ticker.finish();

        assertTrue(output().contains("NORMALIZING"), "label missing from: " + output());
    }
}
