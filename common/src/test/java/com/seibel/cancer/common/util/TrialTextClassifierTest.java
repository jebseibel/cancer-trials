package com.seibel.cancer.common.util;

import com.seibel.cancer.common.enums.DiseaseStage;
import com.seibel.cancer.common.enums.TreatmentGoal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cases are real trials from the 2,473-trial corpus measurement of 2026-08-21, not invented
 * phrasings. The wordings that broke the first pattern set were ones nobody would have guessed,
 * and a test written from imagination would have missed every one of them.
 */
class TrialTextClassifierTest {

    @ParameterizedTest
    @DisplayName("metastasis-directed trials classify as ablative")
    @ValueSource(strings = {
            // NCT03808337 - "metastases", the plural noun, which a `metastatic`-only pattern
            // missed entirely while the distribution looked healthy
            "SBRT delivered to all sites of disease in participants with 1-5 metastases",
            // NCT04563507 - first-line HR+ on letrozole+palbociclib
            "Patients receiving standard first line therapy for metastatic HR+ Breast cancer "
                    + "(letrozole+palbociclib) are randomly assigned to also receive Stereotactic "
                    + "Body Radiation Therapy to each metastatic lesion",
            // NCT04158843
            "Radical local treatment versus palliative treatment for breast cancer patients with "
                    + "primary ipsilateral humerus or sternum oligometastasis",
            // NCT06246968
            "Participants with metastatic breast cancer receive pembrolizumab in combination "
                    + "with cryoablation",
            // NCT06055881
            "This study assesses if metastasis-directed radiation therapy can delay a change in "
                    + "systemic therapy",
            // NCT04079049
            "evaluate local treatment for breast cancer liver metastases, compared to systemic "
                    + "oncological treatment, with resection of oligometastases"
    })
    void ablativeStrategy(String text) {
        assertThat(TrialTextClassifier.classify(text)).isEqualTo(TreatmentGoal.ABLATIVE);
    }

    /**
     * Every false positive on cure language was the word inside its own denial. One token
     * separates these from a genuine statement of intent, which is the same reason embedding
     * similarity cannot read receptor polarity.
     */
    @ParameterizedTest
    @DisplayName("a negated cure word is not treatment goal")
    @ValueSource(strings = {
            // NCT06682793
            "adults with recurrent unresectable, locally advanced, or metastatic (considered "
                    + "non-curative) solid tumors with EGFR expression",
            // NCT07062965
            "Advanced cancer is a term often used to describe cancer that is unlikely to be cured",
            // NCT05601440
            "First line endocrine therapy improve clinical outcomes, but are not curative, and "
                    + "acquired resistance develops"
    })
    void negatedCureIsNotStated(String text) {
        assertThat(TrialTextClassifier.classify(text)).isEqualTo(TreatmentGoal.NOT_STATED);
    }

    @Test
    @DisplayName("a later unnegated cure word is found past an earlier negated one")
    void scansPastNegatedOccurrence() {
        String text = "Metastatic breast cancer remains difficult to cure in the majority of "
                + "cases. This study treats selected patients with curative intent.";
        assertThat(TrialTextClassifier.classify(text)).isEqualTo(TreatmentGoal.CURE_LANGUAGE);
    }

    @Test
    @DisplayName("cure language without ablative strategy is its own tier, not ablative")
    void cureLanguageIsItsOwnTier() {
        // NCT05334459 - a real trial, matching a bone-dominant metastatic disease pattern
        String text = "A multimodal approach, including LRT with curative intent should be "
                + "considered for selected dnMBC patients, especially the subset of bone-only "
                + "metastatic ones";
        assertThat(TrialTextClassifier.classify(text)).isEqualTo(TreatmentGoal.CURE_LANGUAGE);
    }

    /**
     * Response-endpoint vocabulary was measured and excluded: 232 trials use it and five of five
     * hand-checked were describing how an outcome gets measured, not what the study aims at.
     */
    @ParameterizedTest
    @DisplayName("response-endpoint vocabulary is not a treatment goal")
    @ValueSource(strings = {
            "Secondary endpoints are clinical benefit rate (complete response + partial response "
                    + "+ stable disease), overall survival, adverse events",
            "Primary end point 3-years invasive disease free survival",
            "patients achieving pathological complete response after neoadjuvant chemotherapy do "
                    + "have an excellent prognosis"
    })
    void responseEndpointsAreNotStated(String text) {
        assertThat(TrialTextClassifier.classify(text)).isEqualTo(TreatmentGoal.NOT_STATED);
    }

    @Test
    @DisplayName("absent text is not-stated rather than an assumption either way")
    void absentTextIsNotStated() {
        assertThat(TrialTextClassifier.classify(null)).isEqualTo(TreatmentGoal.NOT_STATED);
        assertThat(TrialTextClassifier.classify("   ")).isEqualTo(TreatmentGoal.NOT_STATED);
    }

    @Test
    @DisplayName("the matched phrase comes back as evidence, not just a verdict")
    void evidenceIsReturned() {
        assertThat(TrialTextClassifier.firstAblativePhrase(
                "SBRT to all sites of disease in 1-5 metastases")).isNotBlank();
        assertThat(TrialTextClassifier.firstUnnegatedCure(
                "treated with curative intent")).isNotBlank();
        assertThat(TrialTextClassifier.firstUnnegatedCure(
                "these regimens are not curative")).isNull();
    }

    @Test
    @DisplayName("ranking puts ablative first and silence last")
    void rankingOrder() {
        assertThat(TreatmentGoal.ABLATIVE.rank())
                .isLessThan(TreatmentGoal.CURE_LANGUAGE.rank());
        assertThat(TreatmentGoal.CURE_LANGUAGE.rank())
                .isLessThan(TreatmentGoal.NOT_STATED.rank());
    }

    @Test
    @DisplayName("an unrecognised stored value reads as not-stated rather than throwing")
    void unrecognisedValueIsSafe() {
        assertThat(TreatmentGoal.fromValue("CURATIVE")).isEqualTo(TreatmentGoal.NOT_STATED);
        assertThat(TreatmentGoal.fromValue(null)).isEqualTo(TreatmentGoal.NOT_STATED);
        assertThat(TreatmentGoal.fromValue("ablative")).isEqualTo(TreatmentGoal.ABLATIVE);
    }

    @ParameterizedTest
    @DisplayName("metastatic trials are recognised, including the plural noun")
    @ValueSource(strings = {
            "A study in metastatic breast cancer",
            // NCT03808337 - "metastases", which a `metastatic`-only pattern missed entirely
            "SBRT delivered to all sites of disease in participants with 1-5 metastases",
            "Patients with stage IV breast cancer",
            "recurrent unresectable disease"
    })
    void metastaticStage(String text) {
        assertThat(TrialTextClassifier.classifyStage(text)).isEqualTo(DiseaseStage.METASTATIC);
    }

    @ParameterizedTest
    @DisplayName("early-stage trials are recognised")
    @ValueSource(strings = {
            "Postoperative adjuvant therapy with T-DM1 for one year",
            "neoadjuvant chemotherapy in operable breast cancer",
            "Ductal Carcinoma in Situ (DCIS) active surveillance",
            "patients with stage II disease"
    })
    void earlyStage(String text) {
        assertThat(TrialTextClassifier.classifyStage(text)).isEqualTo(DiseaseStage.EARLY_STAGE);
    }

    /**
     * Without word boundaries these match inside "unresectable" and "inoperable", which mean the
     * opposite. A trial reading "recurrent unresectable ... metastatic" was classified
     * early-stage on that substring during the corpus measurement.
     */
    @ParameterizedTest
    @DisplayName("a negative prefix does not read as early-stage")
    @ValueSource(strings = {
            "recurrent unresectable metastatic disease",
            "inoperable metastatic breast cancer"
    })
    void negativePrefixesAreNotEarlyStage(String text) {
        assertThat(TrialTextClassifier.classifyStage(text)).isEqualTo(DiseaseStage.METASTATIC);
    }

    @Test
    @DisplayName("a trial naming both stages says so rather than picking one")
    void bothStages() {
        assertThat(TrialTextClassifier.classifyStage(
                "adjuvant therapy in early-stage and metastatic breast cancer"))
                .isEqualTo(DiseaseStage.BOTH);
    }

    /**
     * "Locally advanced" is stage III. Matching bare "advanced" as metastatic would admit
     * exactly the early-stage trials this test exists to keep out.
     */
    @Test
    @DisplayName("locally advanced alone is not metastatic")
    void locallyAdvancedIsNotMetastatic() {
        assertThat(TrialTextClassifier.classifyStage(
                "patients with locally advanced breast cancer"))
                .isEqualTo(DiseaseStage.NOT_STATED);
    }

    @Test
    @DisplayName("silence is not-stated, and only a clear early-stage trial is filtered out")
    void silenceAndFilterSafety() {
        assertThat(TrialTextClassifier.classifyStage("A study of drug X versus placebo"))
                .isEqualTo(DiseaseStage.NOT_STATED);
        assertThat(TrialTextClassifier.classifyStage(null)).isEqualTo(DiseaseStage.NOT_STATED);

        // A filter built on an inference must fail towards showing a trial, not hiding one.
        assertThat(DiseaseStage.NOT_STATED.couldIncludeMetastatic()).isTrue();
        assertThat(DiseaseStage.BOTH.couldIncludeMetastatic()).isTrue();
        assertThat(DiseaseStage.EARLY_STAGE.couldIncludeMetastatic()).isFalse();
    }
}
