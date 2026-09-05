package com.seibel.cancer.service.ai;

import java.util.List;

/**
 * The verdict from {@link PhiHeuristicScanner#scanLines}: a document is no longer accepted or
 * rejected as a whole. Instead, each line is judged independently, and {@code cleanedText} is
 * the document with every flagged line removed rather than an existing line kept but redacted -
 * so a line that survives carries no trace of one that did not, and extraction never sees a
 * placeholder to guess around.
 *
 * <p>{@code excludedLines} names what was cut, for the caller to show a reviewer which lines
 * disappeared and why - 1-indexed to match how a person would count lines in the document they
 * pasted, and category labels only, same rule as {@link PhiScanResult#reasons()}: never the
 * matched or excluded text itself.
 */
public record PhiLineScanResult(String cleanedText, List<ExcludedLine> excludedLines) {

    public boolean anyExcluded() {
        return !excludedLines.isEmpty();
    }

    public record ExcludedLine(int lineNumber, List<String> reasons) {
    }
}
