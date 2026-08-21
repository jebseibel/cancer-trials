package com.seibel.cancer.service.ai;

import com.seibel.cancer.common.domain.PatientDiagnosis;
import com.seibel.cancer.common.domain.PatientPriorTreatment;
import com.seibel.cancer.common.domain.PatientVariant;
import com.seibel.cancer.common.domain.Trial;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards what leaves the machine.
 *
 * <p>This is the only path in the application that sends clinical text anywhere, so the tests
 * that matter most here are about the payload rather than the response. A field added to
 * {@code PatientDiagnosis} must not start being transmitted because someone changed a mapper —
 * hence the allowlist, and hence these.
 */
class TrialDiagnosisMatchServiceTest {

    private AiService aiService;
    private TrialDiagnosisMatchService service;

    @BeforeEach
    void setUp() {
        aiService = mock(AiService.class);
        service = new TrialDiagnosisMatchService(aiService);
        when(aiService.generateStructured(anyString(), anyString(), any()))
                .thenReturn(new TrialMatchAssessment());
    }

    private Trial trial() {
        Trial t = new Trial();
        t.setNctId("NCT00000001");
        t.setBriefTitle("A Study in Metastatic Breast Cancer");
        t.setEligibilityCriteria("Inclusion Criteria:\n* HR-positive, HER2-negative disease");
        return t;
    }

    private PatientDiagnosis diagnosis() {
        PatientDiagnosis d = new PatientDiagnosis();
        d.setCancerType("Invasive ductal carcinoma");
        d.setStage("Stage IV");
        d.setErStatus("POSITIVE");
        d.setHer2Status("NEGATIVE");
        return d;
    }

    /** The prompt actually sent, captured from the AI call. */
    private String capturedPrompt() {
        ArgumentCaptor<String> user = ArgumentCaptor.forClass(String.class);
        verify(aiService).generateStructured(anyString(), user.capture(), any());
        return user.getValue();
    }

    @Test
    @DisplayName("the trial's criteria and the record both reach the prompt")
    void sendsTrialAndRecord() {
        service.assess(trial(), diagnosis(), null, null);

        assertThat(capturedPrompt())
                .contains("HR-positive, HER2-negative disease")
                .contains("Stage IV")
                .contains("Invasive ductal carcinoma");
    }

    /**
     * Free text cannot be guaranteed identifier-free. The diagnosis notes field already holds a
     * Ki-67 discrepancy and a drug-date conflict; a clinician's name could land there tomorrow.
     */
    @Test
    @DisplayName("free-text notes are never sent, from any of the three records")
    void neverSendsNotes() {
        PatientDiagnosis d = diagnosis();
        d.setNotes("Ki-67 discrepancy noted by Dr Halvorsen at Memorial");

        PatientVariant v = new PatientVariant();
        v.setPik3caStatus("DETECTED");
        v.setNotes("Sample collected at the Aurora campus");

        PatientPriorTreatment t = new PatientPriorTreatment();
        t.setCdk46Status("CURRENT");
        t.setNotes("Patient mentioned her sister's diagnosis");

        service.assess(trial(), d, v, t);

        assertThat(capturedPrompt())
                .doesNotContain("Halvorsen")
                .doesNotContain("Aurora")
                .doesNotContain("sister")
                .doesNotContain("Memorial");
    }

    /**
     * A date more precise than a year is one of HIPAA's eighteen identifiers, and no eligibility
     * criterion needs the day.
     */
    @Test
    @DisplayName("dates are coarsened to a year")
    void coarsensDates() {
        PatientDiagnosis d = diagnosis();
        d.setDiagnosisDate(LocalDate.of(2026, 3, 16));

        PatientPriorTreatment t = new PatientPriorTreatment();
        t.setLastTreatmentEndDate(LocalDate.of(2026, 5, 7));

        service.assess(trial(), d, null, t);

        String prompt = capturedPrompt();
        assertThat(prompt).contains("2026");
        assertThat(prompt).doesNotContain("2026-03-16").doesNotContain("2026-05-07");
    }

    /** The lab names an institution, which narrows a population. */
    @Test
    @DisplayName("the testing lab is not sent")
    void neverSendsTestLab() {
        PatientVariant v = new PatientVariant();
        v.setPik3caStatus("DETECTED");
        v.setTestLab("Ambry Genetics");

        service.assess(trial(), diagnosis(), v, null);

        assertThat(capturedPrompt()).doesNotContain("Ambry");
    }

    /**
     * The five-state vocabularies only work if the model is told what they mean. "Not tested" and
     * "not detected" are different answers and a trial's criteria turn on the difference.
     */
    @Test
    @DisplayName("the record explains its own vocabularies")
    void explainsVocabularies() {
        PatientVariant v = new PatientVariant();
        v.setPik3caStatus("DETECTED");
        PatientPriorTreatment t = new PatientPriorTreatment();
        t.setCdk46Status("CURRENT");

        service.assess(trial(), diagnosis(), v, t);

        String prompt = capturedPrompt();
        assertThat(prompt).contains("NOT_TESTED and NOT_DETECTED are different answers");
        assertThat(prompt).contains("CURRENT and PROGRESSED are different situations");
    }

    @Test
    @DisplayName("an absent field contributes nothing rather than an empty label")
    void skipsAbsentFields() {
        service.assess(trial(), diagnosis(), null, null);

        // PR status was never set, so the model must not see an empty heading it might read as
        // a stated negative.
        assertThat(capturedPrompt()).doesNotContain("PR status");
    }

    @Test
    @DisplayName("no diagnosis on file is explained, not sent")
    void refusesWithoutDiagnosis() {
        assertThatThrownBy(() -> service.assess(trial(), null, null, null))
                .isInstanceOf(AiGenerationException.class)
                .hasMessageContaining("Diagnosis tab");

        verify(aiService, never()).generateStructured(anyString(), anyString(), any());
    }

    @Test
    @DisplayName("a trial with no criteria is refused rather than sent for guesswork")
    void refusesWithoutCriteria() {
        Trial t = trial();
        t.setEligibilityCriteria(null);

        assertThatThrownBy(() -> service.assess(t, diagnosis(), null, null))
                .isInstanceOf(AiGenerationException.class);

        verify(aiService, never()).generateStructured(anyString(), anyString(), any());
    }

    /**
     * The response type is the enforcement point for the no-verdicts rule: if a field named
     * anything like "eligible" ever appears, the model can assert eligibility and the prompt
     * alone will not stop it being rendered.
     */
    @Test
    @DisplayName("the response type cannot carry an eligibility verdict")
    void responseTypeHasNoEligibilityField() {
        assertThat(TrialMatchAssessment.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .noneMatch(name -> {
                    String n = name.toLowerCase();
                    return n.contains("eligib") || n.contains("qualif") || n.contains("score")
                            || n.contains("match");
                });
    }
}
