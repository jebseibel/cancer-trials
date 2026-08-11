package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

/**
 * One assessed eligibility signal, as sent to the frontend.
 *
 * <p>{@code evidence} is the field that makes this honest. The tool's job is to surface what to
 * look into and ask about, so a reader has to see <em>why</em> a trial was flagged and judge the
 * reasoning themselves. A flag with no quotable criteria text is an unexplained verdict, which
 * is exactly what this project does not produce.
 */
@Data
@Builder
public class ResponseEligibilitySignal {

    /** Short label shown to the reader, e.g. "Receptor status". */
    private String name;

    /** PASS, CONCERN, UNKNOWN or NOT_APPLICABLE. */
    private String outcome;

    /** One sentence a non-clinician can read, stating what was compared. */
    private String detail;

    /** The phrase from the trial's criteria that drove this, or null. */
    private String evidence;
}
