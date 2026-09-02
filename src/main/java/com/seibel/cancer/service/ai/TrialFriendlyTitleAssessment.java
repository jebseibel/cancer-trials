package com.seibel.cancer.service.ai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import lombok.Data;

/**
 * A plain-language rewrite of one trial's title, in four fixed parts.
 *
 * <p><b>The field names and descriptions here are part of the prompt.</b> They are sent to the
 * model as a schema, so they read as instructions rather than as notes to a developer.
 *
 * <p>Four separate fields rather than one free-text title, so the caller controls the join
 * ({@code " - "}) and the order rather than trusting the model to format consistently across
 * thousands of trials. See {@link #toFriendlyTitle()}.
 */
@Data
public class TrialFriendlyTitleAssessment {

    @JsonPropertyDescription(
            "The disease stage this trial is for, in plain language a patient would recognise - "
                    + "for example \"Stage IV\" or \"Early-Stage\" or \"Any Stage\". Two to four "
                    + "words. Base this only on what the trial's own text says; if it is not "
                    + "stated, write \"Stage Not Specified\".")
    private String cancerStage;

    @JsonPropertyDescription(
            "Whether the trial is aiming to cure or remove the cancer (\"Curative Intent\"), or "
                    + "to control or slow it without claiming a cure (\"Disease Control\"). If "
                    + "the trial's own text does not say, write \"Goal Not Stated\". Two to "
                    + "three words, no explanation.")
    private String treatmentGoalLabel;

    @JsonPropertyDescription(
            "What the trial is actually trying, in plain non-technical language a patient could "
                    + "say out loud - the drug, drug class, or approach being tested, and "
                    + "whether it is new, added to an existing treatment, or a different "
                    + "combination. For example \"Testing a new SERD pill\" or \"Adding an "
                    + "immunotherapy drug to chemo\". One short phrase, no dosing detail, no "
                    + "trial-design jargon like \"randomized\" or \"open-label\".")
    private String interventionSummary;

    @JsonPropertyDescription(
            "The genomic markers, mutations, or biomarker status a patient would need to even "
                    + "be considered for this trial, in plain language - for example \"Requires "
                    + "a PIK3CA mutation\" or \"ER-positive, HER2-negative\". If the eligibility "
                    + "criteria name no specific marker or mutation, write \"No Specific Marker "
                    + "Required\". One short phrase.")
    private String markersNeeded;

    /**
     * Joins the four parts into the single stored string, in a fixed order the model does not
     * control.
     *
     * <p>Truncated defensively to {@code trial.friendly_title}'s 500-character column width. A
     * model response is never trusted to respect a length instruction on its own.
     */
    public String toFriendlyTitle() {
        String joined = String.join(" - ",
                blankToPlaceholder(cancerStage, "Stage Not Specified"),
                blankToPlaceholder(treatmentGoalLabel, "Goal Not Stated"),
                blankToPlaceholder(interventionSummary, "Approach Not Described"),
                blankToPlaceholder(markersNeeded, "No Specific Marker Required"));
        return joined.length() > 500 ? joined.substring(0, 500) : joined;
    }

    private String blankToPlaceholder(String value, String placeholder) {
        return (value == null || value.isBlank()) ? placeholder : value.strip();
    }
}
