package com.seibel.cancer.datafetcher.normalization;

/**
 * Seam for source-specific parsing. Each trial source (ClinicalTrials.gov, a future
 * Playwright scraper, etc.) implements this and is dispatched to by trial_source.code -
 * TrialNormalizationService doesn't need to know which source a staging row came from.
 */
public interface TrialSourceParser {

    boolean supports(String trialSourceCode);

    NormalizedTrial parse(String rawPayloadJson);
}
