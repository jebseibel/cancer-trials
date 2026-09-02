package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * One semantically matched trial, with the chunks that matched.
 *
 * <p>Exposes extid only, never the internal numeric id, per the project's extid-only rule.
 */
@Data
@Builder
public class ResponseTrialSearchMatch {

    private String trialExtid;
    private String nctId;
    private String briefTitle;

    /** A plain-language rewrite of {@link #briefTitle}, or null when not yet generated. */
    private String friendlyTitle;
    private String overallStatus;

    /** Best similarity score among this trial's matched chunks. */
    private double topScore;

    /** The matched text, best first - this is what an answer cites. */
    private List<Match> matches;

    @Data
    @Builder
    public static class Match {
        private String text;
        /** Which field it came from: INCLUSION_CRITERION, OUTCOME, BRIEF_SUMMARY, ... */
        private String source;
        private int ordinal;
        private double score;
        /**
         * True when this is an exclusion criterion. A high score here suggests the patient may
         * be <b>disqualified</b> - the opposite of a fit - so it must not be read as evidence
         * of eligibility.
         */
        private boolean isExclusion;
    }
}
