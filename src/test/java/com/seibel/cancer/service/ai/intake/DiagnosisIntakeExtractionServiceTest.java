package com.seibel.cancer.service.ai.intake;

import com.seibel.cancer.service.ai.AiService;
import com.seibel.cancer.service.ai.PhiDetectedException;
import com.seibel.cancer.service.ai.PhiHeuristicScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The load-bearing test for this feature: proves the PHI gate runs before {@link AiService} is
 * ever touched, and that the extraction-target allowlist in {@link DiagnosisIntakeExtraction}
 * is respected.
 */
class DiagnosisIntakeExtractionServiceTest {

    private AiService aiService;
    private DiagnosisIntakeExtractionService service;

    @BeforeEach
    void setUp() {
        aiService = mock(AiService.class);
        service = new DiagnosisIntakeExtractionService(aiService, new PhiHeuristicScanner());
    }

    @Test
    @DisplayName("a document flagged by the PHI gate never reaches AiService")
    void gateRunsBeforeAiServiceCall() {
        String flaggedText = "Patient Name: Jane Doe\nDOB: 03/14/1965\nMRN: 00482913\n"
                + "Diagnosis: invasive ductal carcinoma.";

        assertThatThrownBy(() -> service.extract(flaggedText))
                .isInstanceOf(PhiDetectedException.class);

        verifyNoInteractions(aiService);
    }

    @Test
    @DisplayName("the rejection message never echoes the flagged content")
    void rejectionMessageDoesNotEchoContent() {
        String flaggedText = "Patient Name: Jane Doe\nDOB: 03/14/1965";

        assertThatThrownBy(() -> service.extract(flaggedText))
                .isInstanceOf(PhiDetectedException.class)
                .hasMessageNotContaining("Jane")
                .hasMessageNotContaining("Doe")
                .hasMessageNotContaining("03/14/1965");
    }

    @Test
    @DisplayName("clean text passes the gate and reaches AiService")
    void cleanTextReachesAiService() {
        when(aiService.generateStructured(anyString(), anyString(), any()))
                .thenReturn(new DiagnosisIntakeExtraction());

        String cleanText = "Diagnosis: invasive ductal carcinoma, Stage II. ER positive, "
                + "PR positive, HER2 negative. ECOG 0.";

        service.extract(cleanText);

        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        verify(aiService).generateStructured(anyString(), userPrompt.capture(), any());
        assertThat(userPrompt.getValue()).contains("invasive ductal carcinoma");
    }

    @Test
    @DisplayName("missingRequired reports every unresolved required field")
    void missingRequiredReportsAllGaps() {
        DiagnosisIntakeExtraction draft = new DiagnosisIntakeExtraction();

        assertThat(service.missingRequired(draft))
                .containsExactlyInAnyOrder("cancerType", "stage", "erStatus", "prStatus",
                        "her2Status", "ecogStatus");
    }

    @Test
    @DisplayName("an UNKNOWN receptor status counts as missing, not resolved")
    void unknownReceptorStatusCountsAsMissing() {
        DiagnosisIntakeExtraction draft = new DiagnosisIntakeExtraction();
        draft.setCancerType("Invasive ductal carcinoma");
        draft.setStage("II");
        draft.setErStatus("UNKNOWN");
        draft.setPrStatus("POSITIVE");
        draft.setHer2Status("NEGATIVE");
        draft.setEcogStatus(0);

        assertThat(service.missingRequired(draft)).containsExactly("erStatus");
    }

    @Test
    @DisplayName("all required fields present leaves nothing missing")
    void allRequiredFieldsPresentLeavesNothingMissing() {
        DiagnosisIntakeExtraction draft = new DiagnosisIntakeExtraction();
        draft.setCancerType("Invasive ductal carcinoma");
        draft.setStage("II");
        draft.setErStatus("POSITIVE");
        draft.setPrStatus("POSITIVE");
        draft.setHer2Status("NEGATIVE");
        draft.setEcogStatus(0);

        assertThat(service.missingRequired(draft)).isEmpty();
    }

    @Test
    @DisplayName("clarify sends the draft, the gap list, the question, and the answer")
    void clarifySendsDraftGapsQuestionAndAnswer() {
        when(aiService.generateStructured(anyString(), anyString(), any()))
                .thenReturn(new DiagnosisIntakeClarification());

        DiagnosisIntakeExtraction draft = new DiagnosisIntakeExtraction();
        draft.setCancerType("Invasive ductal carcinoma");

        service.clarify(draft, List.of("her2Status"),
                "Is the tumor HER2 positive, negative, or not tested?",
                "It's triple negative");

        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        verify(aiService).generateStructured(anyString(), userPrompt.capture(), any());
        assertThat(userPrompt.getValue())
                .contains("Invasive ductal carcinoma")
                .contains("her2Status")
                .contains("Is the tumor HER2 positive, negative, or not tested?")
                .contains("It's triple negative");
    }
}
