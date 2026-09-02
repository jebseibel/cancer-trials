package com.seibel.cancer.service.ai;

import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.database.db.service.TrialDbService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unlike {@link TrialDiagnosisMatchServiceTest}, there is no allowlist to guard here — this path
 * never receives a patient record at all, so the tests that matter are about the trial text
 * reaching the prompt and the result always being written.
 */
class TrialFriendlyTitleServiceTest {

    private AiService aiService;
    private TrialDbService trialDbService;
    private TrialFriendlyTitleService service;

    @BeforeEach
    void setUp() {
        aiService = mock(AiService.class);
        trialDbService = mock(TrialDbService.class);
        service = new TrialFriendlyTitleService(aiService, trialDbService);

        TrialFriendlyTitleAssessment generated = new TrialFriendlyTitleAssessment();
        generated.setCancerStage("Stage IV");
        generated.setTreatmentGoalLabel("Disease Control");
        generated.setInterventionSummary("Adding a new drug");
        generated.setMarkersNeeded("No Specific Marker Required");
        when(aiService.generateStructured(anyString(), anyString(), any()))
                .thenReturn(generated);

        when(trialDbService.update(anyString(), any())).thenReturn(new Trial());
    }

    private Trial trial() {
        Trial t = new Trial();
        t.setExtid("trial-extid");
        t.setNctId("NCT00000001");
        t.setBriefTitle("A Phase 3 Randomized Study of Drug X in Metastatic Breast Cancer");
        t.setEligibilityCriteria("Inclusion Criteria:\n* PIK3CA mutation required");
        return t;
    }

    private String capturedPrompt() {
        ArgumentCaptor<String> user = ArgumentCaptor.forClass(String.class);
        verify(aiService).generateStructured(anyString(), user.capture(), any());
        return user.getValue();
    }

    @Test
    @DisplayName("the trial's own text reaches the prompt")
    void sendsTrialText() {
        service.generate(trial());

        assertThat(capturedPrompt())
                .contains("A Phase 3 Randomized Study of Drug X in Metastatic Breast Cancer")
                .contains("PIK3CA mutation required");
    }

    @Test
    @DisplayName("the four generated parts are joined and written to the trial")
    void writesJoinedTitle() {
        service.generate(trial());

        ArgumentCaptor<Trial> patch = ArgumentCaptor.forClass(Trial.class);
        verify(trialDbService).update(eq("trial-extid"), patch.capture());
        assertThat(patch.getValue().getFriendlyTitle())
                .isEqualTo("Stage IV - Disease Control - Adding a new drug - "
                        + "No Specific Marker Required");
    }

    @Test
    @DisplayName("a trial with no title at all is refused rather than sent for guesswork")
    void refusesWithoutAnyTitle() {
        Trial t = new Trial();
        t.setExtid("x");

        assertThatThrownBy(() -> service.generate(t))
                .isInstanceOf(AiGenerationException.class);

        verify(aiService, never()).generateStructured(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("a null trial is refused")
    void refusesNullTrial() {
        assertThatThrownBy(() -> service.generate(null))
                .isInstanceOf(AiGenerationException.class);
    }
}
