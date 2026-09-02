package com.seibel.cancer.service.ai;

import java.util.List;

/**
 * The verdict from {@link PhiHeuristicScanner#scan}.
 *
 * <p>{@code reasons} is category labels only - never the matched substring. A rejection message
 * or a log line built from this record must not itself repeat whatever the scan caught.
 */
public record PhiScanResult(boolean flagged, List<String> reasons) {
}
