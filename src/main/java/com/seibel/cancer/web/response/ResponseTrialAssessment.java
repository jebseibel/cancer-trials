package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * One trial with every signal assessed against the patient's record.
 *
 * <p><b>There is deliberately no fit score or percentage.</b> The previous attempt scored
 * {@code signals_matched / 6}, which was unreachable by construction — a trial cannot be both
 * "first-line treatment-naive" and "post-CDK4/6 progression" — and worse, it counted keyword
 * co-occurrence in criteria text rather than whether the patient qualifies. A number that looks
 * like a probability but is not one invites exactly the reliance this tool must not encourage.
 *
 * <p>What is exposed instead is countable and honest: how many signals raised a concern, how
 * many are open questions, and how many the trial spoke to at all. A caller can order by those
 * without anyone reading a "73% match" that means nothing.
 *
 * <p>Enough trial detail is carried inline to render a result row without a second call. Trial
 * extid is the identifier; no internal numeric id crosses this boundary.
 */
@Data
@Builder
public class ResponseTrialAssessment {

    private String trialExtid;
    private String nctId;
    private String briefTitle;
    private String overallStatus;

    /** Every signal, including NOT_APPLICABLE ones, in a stable order. */
    private List<ResponseEligibilitySignal> signals;

    /** Signals that raised a concern. The primary demotion key — never a removal key. */
    private long concernCount;

    /** Signals that applied but could not be resolved. These are questions, not failures. */
    private long unknownCount;

    /** Signals the patient meets. */
    private long passCount;

    /**
     * Signals this trial actually spoke to. The denominator worth reporting, when one is
     * reported at all: a trial silent on three of four signals has not passed three checks.
     */
    private long applicableCount;

    /**
     * Whether the trial appears to be about breast cancer.
     *
     * <p>Surfaced separately because it is the one signal a caller may want to filter or group
     * on rather than merely rank by — the corpus is only 45.7% breast, so a list that ignores it
     * is more than half trials for another disease. It stays a flag rather than a filter here;
     * hiding a trial is the caller's decision, not this layer's.
     */
    private boolean breastCancer;

    /**
     * Where the trial actually runs, as "City, State" strings.
     *
     * <p>A first-class field rather than something to be read out of the location signal's
     * sentence. Travel decides whether a trial is possible at all for most people — a perfect
     * biological match in another state may be out of reach, and a mediocre one nearby may not
     * be. Anything the reader needs in order to judge that has to be on the card, not behind a
     * toggle.
     *
     * <p>US sites when there are any; otherwise the countries the trial runs in, so an
     * international-only trial says so plainly instead of appearing to have no locations.
     */
    private List<String> siteCities;

    /** Total distinct sites, since {@link #siteCities} is capped for display. */
    private int siteCount;

    /** False when the trial has no United States site — a travel flag, not a disqualifier. */
    private boolean hasUnitedStatesSite;
}
