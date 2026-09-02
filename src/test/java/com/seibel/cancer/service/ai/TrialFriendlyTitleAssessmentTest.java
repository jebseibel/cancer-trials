package com.seibel.cancer.service.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The join happens here, not in the model's own formatting — these tests are what makes that
 * true regardless of what the prompt asks for.
 */
class TrialFriendlyTitleAssessmentTest {

    private TrialFriendlyTitleAssessment assessment(String stage, String goal, String intervention,
                                                     String markers) {
        TrialFriendlyTitleAssessment a = new TrialFriendlyTitleAssessment();
        a.setCancerStage(stage);
        a.setTreatmentGoalLabel(goal);
        a.setInterventionSummary(intervention);
        a.setMarkersNeeded(markers);
        return a;
    }

    @Test
    @DisplayName("the four parts are joined with a dash in a fixed order")
    void joinsInFixedOrder() {
        TrialFriendlyTitleAssessment a = assessment(
                "Stage IV", "Disease Control", "Adding a CDK4/6 inhibitor",
                "Requires ER-positive disease");

        assertThat(a.toFriendlyTitle())
                .isEqualTo("Stage IV - Disease Control - Adding a CDK4/6 inhibitor - "
                        + "Requires ER-positive disease");
    }

    @Test
    @DisplayName("a blank part is replaced with its own placeholder, not dropped")
    void blankPartsBecomePlaceholders() {
        TrialFriendlyTitleAssessment a = assessment(null, "", "  ", "No Specific Marker Required");

        assertThat(a.toFriendlyTitle())
                .isEqualTo("Stage Not Specified - Goal Not Stated - Approach Not Described - "
                        + "No Specific Marker Required");
    }

    @Test
    @DisplayName("the joined string never exceeds the friendly_title column width")
    void truncatesToColumnWidth() {
        String long500 = "X".repeat(200);
        TrialFriendlyTitleAssessment a = assessment(long500, long500, long500, long500);

        assertThat(a.toFriendlyTitle()).hasSizeLessThanOrEqualTo(500);
    }
}
