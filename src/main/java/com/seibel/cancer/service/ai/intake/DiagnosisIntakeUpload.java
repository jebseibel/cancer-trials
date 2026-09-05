package com.seibel.cancer.service.ai.intake;

import com.seibel.cancer.service.ai.PhiLineScanResult;

import java.util.List;

/**
 * The result of {@link DiagnosisIntakeExtractionService#extract}: the draft extracted from
 * whatever survived the PHI line scan, plus which lines did not survive it and why.
 *
 * <p>{@code excludedLines} is 1-indexed against the document the user submitted, and its reasons
 * are category labels only - same rule as everywhere else in this feature, the excluded text
 * itself is never carried anywhere past the scan that found it.
 */
public record DiagnosisIntakeUpload(
        DiagnosisIntakeExtraction draft,
        List<PhiLineScanResult.ExcludedLine> excludedLines) {
}
