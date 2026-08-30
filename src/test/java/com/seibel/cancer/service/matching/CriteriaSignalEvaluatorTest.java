package com.seibel.cancer.service.matching;

import com.seibel.cancer.common.domain.PatientDiagnosis;
import com.seibel.cancer.common.domain.PatientPriorTreatment;
import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.common.domain.matching.EligibilitySignal;
import com.seibel.cancer.common.domain.matching.SignalOutcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Receptor-polarity behaviour of {@link CriteriaSignalEvaluator}.
 *
 * <p>These cases are the evaluator's reason for existing. Retrieval ranked a triple-negative
 * trial first for an ER-positive patient on 2026-08-08, because "HR-negative, HER2-negative"
 * and "HR-positive, HER2-negative" differ by one token and embeddings score them alike. Every
 * assertion here is a phrasing that appeared in real criteria text or a real pathology report,
 * not an invented string.
 *
 * <p>The patient this was measured against has a real, de-identified clinical profile — so the
 * "must not flag" cases matter as much as the "must flag" ones.
 */
class CriteriaSignalEvaluatorTest {

    private final CriteriaSignalEvaluator evaluator = new CriteriaSignalEvaluator();

    /** The profile this suite is measured against: hormone-receptor positive, HER2 negative. */
    private PatientDiagnosis hrPositiveHer2Negative() {
        PatientDiagnosis d = new PatientDiagnosis();
        d.setErStatus("POSITIVE");
        d.setPrStatus("NEGATIVE");
        d.setHer2Status("NEGATIVE");
        return d;
    }

    private Trial trialWithCriteria(String criteria) {
        Trial t = new Trial();
        t.setEligibilityCriteria(criteria);
        return t;
    }

    private EligibilitySignal assess(String criteria) {
        return evaluator.receptorSignal(trialWithCriteria(criteria), hrPositiveHer2Negative());
    }

    @Nested
    @DisplayName("hormone-receptor-positive phrasing")
    class HormonePositive {

        /**
         * The pattern originally matched only HR/ER/hormone receptor, so criteria spelling the
         * receptor out scored NOT_APPLICABLE — a missed PASS on the patient's most important
         * axis.
         */
        @ParameterizedTest
        @ValueSource(strings = {
                "estrogen receptor positive breast cancer",
                "estrogen receptor (ER)-positive tumors",
                "oestrogen receptor positive disease",
                "progesterone receptor positive",
                "estrogen-receptor-positive",
                "PgR positive",
                "ER+ / HER2- metastatic breast cancer",
                "hormone receptor positive",
                "hormone receptor-positive (HR+)",
                "ER-positive breast cancer"
        })
        void passesWhenTrialWantsHormonePositiveDisease(String criteria) {
            assertThat(assess(criteria).outcome()).isEqualTo(SignalOutcome.PASS);
        }

        /** A negative phrasing must never be read as a positive requirement. */
        @ParameterizedTest
        @ValueSource(strings = {
                "estrogen receptor negative",
                "progesterone receptor negative, <1%",
                "ER negative and PR negative"
        })
        void doesNotTreatReceptorNegativePhrasingAsPositive(String criteria) {
            assertThat(assess(criteria).outcome()).isNotEqualTo(SignalOutcome.PASS);
        }
    }

    @Nested
    @DisplayName("HER2 polarity")
    class Her2Polarity {

        /**
         * "HER2-" followed by a space is the shorthand this branch exists for, and a hyphen
         * before whitespace is not a word boundary — so the original {@code -\b} alternative
         * never fired for it.
         */
        @ParameterizedTest
        @ValueSource(strings = {
                "HER2- metastatic breast cancer",
                "ER+/HER2- disease",
                "HER2-negative",
                "HER2 negative",
                "HER2 non-amplified by ISH"
        })
        void passesWhenTrialWantsHer2NegativeDisease(String criteria) {
            assertThat(assess(criteria).outcome()).isEqualTo(SignalOutcome.PASS);
        }

        /**
         * A HER2-low patient (IHC 1+, not amplified) is still HER2-negative for eligibility
         * purposes. A trial requiring HER2-positive disease is a genuine mismatch worth raising.
         */
        @Test
        void flagsATrialRequiringHer2PositiveDisease() {
            EligibilitySignal signal = assess("Patients must have HER2-positive breast cancer");
            assertThat(signal.outcome()).isEqualTo(SignalOutcome.CONCERN);
            assertThat(signal.evidence()).containsIgnoringCase("HER2-positive");
        }

        /** "non-amplified"/"not amplified" must not read as a HER2-positive requirement. */
        @ParameterizedTest
        @ValueSource(strings = {
                "HER2 non-amplified by ISH",
                "HER2 not amplified, ratio 1.1"
        })
        void doesNotReadNonAmplifiedAsHer2Positive(String criteria) {
            assertThat(assess(criteria).outcome()).isNotEqualTo(SignalOutcome.CONCERN);
        }
    }

    @Nested
    @DisplayName("triple-negative")
    class TripleNegative {

        /**
         * The 2026-08-08 mis-ranking, as a test. NCT06685796 scored highest for a
         * hormone-positive patient on "HR-negative, HER2-negative" — a different disease subtype.
         */
        @ParameterizedTest
        @ValueSource(strings = {
                "Patients with HR-negative, HER2-negative (triple negative) breast cancer",
                "Inclusion: triple-negative breast cancer",
                "Histologically confirmed TNBC"
        })
        void flagsTripleNegativeTrialsForAHormonePositivePatient(String criteria) {
            EligibilitySignal signal = assess(criteria);
            assertThat(signal.outcome()).isEqualTo(SignalOutcome.CONCERN);
            assertThat(signal.evidence()).isNotBlank();
        }
    }

    /**
     * The exclusion-context check.
     *
     * <p>Every case here inverts under a section header. Before the split existed the evaluator
     * read criteria as one flat string, so "triple-negative breast cancer" produced a CONCERN
     * whether the trial required that subtype or ruled it out — and ruling it out makes the
     * trial a fit for this patient. A false concern demotes a trial she could join.
     *
     * <p>Real criteria text always carries the headers; these fixtures use the shape the
     * chunker was tuned against — asterisk bullets, not the hyphens that appear in no real
     * record.
     */
    @Nested
    @DisplayName("inclusion vs exclusion context")
    class ExclusionContext {

        private String withSections(String inclusion, String exclusion) {
            return "Inclusion Criteria:\n\n* " + inclusion
                    + "\n\nExclusion Criteria:\n\n* " + exclusion;
        }

        /** A trial that rules out triple-negative disease is a trial FOR this patient. */
        @Test
        void doesNotFlagATrialThatExcludesTripleNegativeDisease() {
            EligibilitySignal signal = assess(withSections(
                    "Metastatic breast cancer",
                    "Patients with triple-negative breast cancer"));
            assertThat(signal.outcome()).isNotEqualTo(SignalOutcome.CONCERN);
        }

        /** Requiring it is still the mis-ranking case, and must still flag. */
        @Test
        void stillFlagsATrialThatRequiresTripleNegativeDisease() {
            EligibilitySignal signal = assess(withSections(
                    "Histologically confirmed triple-negative breast cancer",
                    "Prior chemotherapy for metastatic disease"));
            assertThat(signal.outcome()).isEqualTo(SignalOutcome.CONCERN);
        }

        /**
         * The largest source of false concerns: excluding HER2-positive disease is how many
         * trials state a HER2-negative requirement.
         */
        @Test
        void readsExcludedHer2PositiveAsAHer2NegativeRequirement() {
            EligibilitySignal signal = assess(withSections(
                    "Metastatic breast cancer",
                    "Patients with HER2-positive disease are excluded"));
            assertThat(signal.outcome()).isEqualTo(SignalOutcome.PASS);
        }

        /**
         * The guard that used to suppress the false concern was {@code !her2NegAlsoMentioned} —
         * it only worked when the trial happened to write "HER2-negative" somewhere too. This
         * is the case that slipped through: an exclusion with no such phrase anywhere.
         */
        @Test
        void doesNotFlagHer2PositiveExclusionWhenNoNegativePhraseAppears() {
            EligibilitySignal signal = assess(withSections(
                    "Adults with advanced solid tumors",
                    "Known HER2-amplified disease"));
            assertThat(signal.outcome()).isNotEqualTo(SignalOutcome.CONCERN);
        }

        /**
         * NCT05894239 — requires "documenting HER2-positivity" and mentions HER2-negative in an
         * unrelated inclusion line. The old `!her2NegAlsoMentioned` guard was section-wide, so
         * the second suppressed the first: a HER2-positive trial reported four passes and zero
         * concerns for this HER2-negative patient and ranked 4th in the first live run.
         */
        @Test
        void flagsHer2PositiveRequirementDespiteAnUnrelatedHer2NegativeMention() {
            EligibilitySignal signal = assess(withSections(
                    "Confirmation of HER2 biomarker eligibility based on valid results from "
                            + "central testing of tumor tissue documenting HER2-positivity"
                            + "\n* Prior therapy in the HER2-negative setting is permitted",
                    "Uncontrolled brain metastases"));
            assertThat(signal.outcome()).isEqualTo(SignalOutcome.CONCERN);
        }

        /**
         * A single criterion naming both polarities is stating a comparison, not a requirement,
         * and must not be read as demanding either.
         */
        @Test
        void doesNotFlagACriterionThatNamesBothPolarities() {
            EligibilitySignal signal = assess(withSections(
                    "HER2-negative or HER2-positive disease by local laboratory testing",
                    "Uncontrolled brain metastases"));
            assertThat(signal.outcome()).isNotEqualTo(SignalOutcome.CONCERN);
        }

        /** Requiring HER2-positive disease under Inclusion is a genuine mismatch. */
        @Test
        void stillFlagsATrialThatRequiresHer2PositiveDisease() {
            EligibilitySignal signal = assess(withSections(
                    "HER2-positive metastatic breast cancer",
                    "Prior trastuzumab"));
            assertThat(signal.outcome()).isEqualTo(SignalOutcome.CONCERN);
        }

        /**
         * A criteria block with no headers cannot have its polarity recovered, so it is read as
         * inclusion — the pre-existing behaviour. Legacy "DISEASE CHARACTERISTICS:" records
         * never stated a division, and inventing one would invert text that never said so.
         */
        @Test
        void readsUnparsedCriteriaAsInclusionRatherThanGuessing() {
            EligibilitySignal signal = evaluator.receptorSignal(
                    trialWithCriteria("DISEASE CHARACTERISTICS: triple-negative breast cancer"),
                    hrPositiveHer2Negative());
            assertThat(signal.outcome()).isEqualTo(SignalOutcome.CONCERN);
        }
    }

    /**
     * Treatment-line polarity, against the profile this suite is measured against: on a CDK4/6
     * inhibitor now, no progression on it, and no cytotoxic chemotherapy ever.
     */
    @Nested
    @DisplayName("treatment history in context")
    class TreatmentHistory {

        private PatientPriorTreatment onCdk46NotProgressed() {
            PatientPriorTreatment t = new PatientPriorTreatment();
            t.setCdk46Status("CURRENT");
            return t;
        }

        private EligibilitySignal assessTreatment(String inclusion, String exclusion) {
            return evaluator.treatmentLineSignal(
                    trialWithCriteria("Inclusion Criteria:\n\n* " + inclusion
                            + "\n\nExclusion Criteria:\n\n* " + exclusion),
                    onCdk46NotProgressed());
        }

        /**
         * Prior CDK4/6 under Exclusion is a bar, not a requirement. The section-blind version
         * read this as a post-CDK4/6 trial and reported a CONCERN about being on one now —
         * right outcome, wrong reason. It is a concern because she is excluded.
         */
        @ParameterizedTest
        @ValueSource(strings = {
                "Prior treatment with a CDK4/6 inhibitor",
                "Any prior therapy with a CDK4/6 inhibitor",
                "Previous treatment with a CDK 4/6 inhibitor",
                "Received a CDK4/6 inhibitor",
                "Treatment with a CDK4/6 inhibitor within 4 weeks",
                "prior CDK4/6 inhibitor"
        })
        void flagsATrialThatExcludesPriorCdk46Exposure(String exclusion) {
            EligibilitySignal signal = assessTreatment("Metastatic breast cancer", exclusion);
            assertThat(signal.outcome()).isEqualTo(SignalOutcome.CONCERN);
            assertThat(signal.detail()).containsIgnoringCase("exclude");
        }

        /**
         * Naming the class is only disqualifying under Exclusion. Under Inclusion the same
         * words are a requirement, and this patient — on abemaciclib now — meets a prior-CDK4/6
         * requirement rather than being barred by it.
         */
        @Test
        void doesNotReportAnExclusionWhenTheClassIsNamedUnderInclusion() {
            EligibilitySignal signal = assessTreatment(
                    "Prior treatment with a CDK4/6 inhibitor",
                    "Uncontrolled brain metastases");
            assertThat(signal.detail()).doesNotContainIgnoringCase("exclude");
        }

        /**
         * Criteria text from real trials the 2026-08-11 corpus sweep flagged wrongly.
         *
         * <p>{@code TREATMENT_NAIVE} matched "untreated" and "first-line" anywhere in a
         * section, so a trial in an unrelated disease was compared against her CDK4/6 history:
         * 550 concerns and zero passes across 4,634 trials. A naive requirement only speaks to
         * her record when it names a therapy class her record covers.
         */
        @ParameterizedTest
        @ValueSource(strings = {
                "Has received no prior treatment for their DLBCL",
                "Relapsed AML Untreated relapse and are not candidates for allogeneic transplant",
                "Untreated with anti-tumor therapy for rectal cancer",
                "Treatment-naive with at least stage IIB disease",
                "Has biopsy-proven, previously untreated, histologically confirmed disease",
                "Metastatic disease, eligible for first-line therapy with 5-FU, oxaliplatin"
        })
        void doesNotCompareCdk46HistoryAgainstUnrelatedDiseaseTreatment(String inclusion) {
            EligibilitySignal signal = assessTreatment(inclusion, "Uncontrolled brain metastases");
            assertThat(signal.outcome()).isEqualTo(SignalOutcome.NOT_APPLICABLE);
        }

        /**
         * NCT07060807 — HR+/HER2- metastatic breast cancer that explicitly admits people
         * previously treated with endocrine therapy plus a CDK4/6 inhibitor. She qualifies, and
         * the section-blind version flagged it as a concern.
         */
        @Test
        void doesNotFlagATrialThatExplicitlyAdmitsPriorCdk46Patients() {
            EligibilitySignal signal = assessTreatment(
                    "Participants previously treated with ET plus a CDK4/6 inhibitor may "
                            + "participate as long as at least one prior line was received",
                    "Has breast cancer amenable to treatment with curative intent");
            assertThat(signal.outcome()).isNotEqualTo(SignalOutcome.CONCERN);
        }

        /**
         * NCT07044310 — requires hormone-receptor-positive disease and that participants are
         * "Receiving or will receive CDK 4/6 inhibitor". This test's patient is receiving one.
         */
        @Test
        void doesNotFlagATrialThatRequiresBeingOnACdk46Inhibitor() {
            EligibilitySignal signal = assessTreatment(
                    "Will be starting on an aromatase inhibitor (letrozole, anastrozole, or "
                            + "exemestane), receiving or will receive CDK 4/6 inhibitor",
                    "Requires prolonged systemic antibiotic therapy");
            assertThat(signal.outcome()).isNotEqualTo(SignalOutcome.CONCERN);
        }

        /** A naive requirement that DOES name her therapy class must still flag. */
        @ParameterizedTest
        @ValueSource(strings = {
                "No prior endocrine therapy for metastatic disease",
                "Treatment-naive to CDK4/6 inhibitors",
                "First-line endocrine therapy for advanced breast cancer"
        })
        void stillFlagsANaiveRequirementThatNamesHerTherapyClass(String inclusion) {
            EligibilitySignal signal = assessTreatment(inclusion, "Uncontrolled brain metastases");
            assertThat(signal.outcome()).isEqualTo(SignalOutcome.CONCERN);
        }

        /**
         * The distinction a boolean destroys: a post-progression trial does not fit someone
         * currently responding to the drug.
         */
        @Test
        void flagsAPostProgressionTrialForSomeoneStillRespondingToTheDrug() {
            EligibilitySignal signal = assessTreatment(
                    "Progression on a prior CDK4/6 inhibitor",
                    "Prior chemotherapy for metastatic disease");
            assertThat(signal.outcome()).isEqualTo(SignalOutcome.CONCERN);
        }
    }

    /**
     * The disease-type gate.
     *
     * <p>The corpus is 45.7% breast — a mistaken first pull loaded 2,500 general-cancer trials —
     * so without this the other signals report 77-88% NOT_APPLICABLE and a ranked list is mostly
     * trials for a disease she does not have.
     */
    @Nested
    @DisplayName("disease type")
    class DiseaseType {

        private Trial trial(String briefTitle, String summary) {
            Trial t = new Trial();
            t.setBriefTitle(briefTitle);
            t.setBriefSummary(summary);
            return t;
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "A Study of Ribociclib in HR+/HER2- Metastatic Breast Cancer",
                "Nipple-Sparing Mastectomy for Breast Cancer",
                "A Trial in Triple-Negative Breast Cancer (TNBC)",
                "Ductal Carcinoma in Situ (DCIS) Active Surveillance",
                "Invasive Mammary Carcinoma Adjuvant Therapy"
        })
        void passesBreastTrials(String title) {
            assertThat(evaluator.diseaseTypeSignal(trial(title, "")).outcome())
                    .isEqualTo(SignalOutcome.PASS);
        }

        /**
         * Surgical and screening studies that describe breast procedures without ever writing
         * "breast" are missed. Measured across the corpus: 10 trials in 4,634, almost all
         * surgical-technique or mammography-outreach studies rather than treatment trials.
         *
         * <p>Adding "mastectomy"/"mammography"/"axillary" as proxies was considered and
         * rejected — the same sweep found one of those 10 is a <em>lung</em> cancer screening
         * study, so the proxies import noise into the one signal whose job is removing it.
         */
        @Test
        void missesBreastSurgeryTrialsThatNeverWriteTheWordBreast() {
            EligibilitySignal signal = evaluator.diseaseTypeSignal(
                    trial("Endoscopy/Robotic Assisted Nipple Skin Sparing Mastectomy", ""));
            assertThat(signal.outcome()).isEqualTo(SignalOutcome.CONCERN);
        }

        /** Real titles from the sweep that the other signals were wastefully assessing. */
        @ParameterizedTest
        @ValueSource(strings = {
                "Evaluating Fruquintinib in Microsatellite Stable Metastatic Colorectal Cancer",
                "A Window Trial of 5-Azacytidine in Resectable HPV-Associated HNSCC",
                "Relapsed AML Treatment Study",
                "Untreated Diffuse Large B-Cell Lymphoma (DLBCL) Study"
        })
        void flagsTrialsForOtherDiseases(String title) {
            EligibilitySignal signal = evaluator.diseaseTypeSignal(trial(title, ""));
            assertThat(signal.outcome()).isEqualTo(SignalOutcome.CONCERN);
            assertThat(signal.evidence()).isNotBlank();
        }

        /**
         * A basket trial lists breast among many tumour types. It may still enrol her, so it is
         * a question rather than a concern — NCT07331532 and NCT07432633 are both this shape.
         */
        @Test
        void reportsUnknownForABasketTrialCoveringManyTumourTypes() {
            EligibilitySignal signal = evaluator.diseaseTypeSignal(trial(
                    "PET Imaging in Selected Oncology Indications",
                    "Patients with breast, colorectal, gastric, ovarian or pancreatic cancer"));
            assertThat(signal.outcome()).isEqualTo(SignalOutcome.UNKNOWN);
        }

        /** The summary carries the disease when the title is a bare drug code. */
        @Test
        void readsTheSummaryWhenTheTitleDoesNotNameTheDisease() {
            EligibilitySignal signal = evaluator.diseaseTypeSignal(trial(
                    "A Phase 1 Study of XYZ-123",
                    "In participants with advanced breast cancer"));
            assertThat(signal.outcome()).isEqualTo(SignalOutcome.PASS);
        }

        /** Never a removal, and never a silent pass when there is nothing to read. */
        @Test
        void reportsUnknownWhenThereIsNoTitleOrSummary() {
            assertThat(evaluator.diseaseTypeSignal(trial(null, null)).outcome())
                    .isEqualTo(SignalOutcome.UNKNOWN);
        }
    }

    @Nested
    @DisplayName("no-verdicts rule")
    class NoVerdicts {

        @Test
        void reportsUnknownRatherThanGuessingWhenReceptorStatusIsUnrecorded() {
            PatientDiagnosis blank = new PatientDiagnosis();
            EligibilitySignal signal = evaluator.receptorSignal(
                    trialWithCriteria("Advanced solid tumors of any histology"), blank);
            assertThat(signal.outcome()).isIn(SignalOutcome.UNKNOWN, SignalOutcome.NOT_APPLICABLE);
        }

        @Test
        void reportsNotApplicableWhenTheTrialSaysNothingAboutReceptors() {
            EligibilitySignal signal = assess("Participants must be 18 years or older");
            assertThat(signal.outcome()).isEqualTo(SignalOutcome.NOT_APPLICABLE);
        }

        /** Every flag has to carry quotable evidence, or it is an unexplained verdict. */
        @Test
        void everyConcernCarriesTheCriteriaPhraseThatProducedIt() {
            assertThat(assess("Inclusion: triple-negative breast cancer").evidence()).isNotBlank();
            assertThat(assess("Must have HER2-positive disease").evidence()).isNotBlank();
        }
    }

    /**
     * Treatment goal and disease stage — the two signals about what a trial is trying to do.
     *
     * <p>Cases are drawn from the corpus measurement of 2026-08-21 rather than invented, because
     * the phrasings that broke the first pattern set were ones nobody would have guessed:
     * "1-5 metastases" rather than "metastatic", and cure words appearing inside their own
     * denial. See matching/CURATIVE_STEP1_MEASUREMENT.md.
     */
    @Nested
    @DisplayName("treatment goal and disease stage")
    class CurativeIntent {

        private Trial trial(String summary) {
            Trial t = new Trial();
            t.setBriefTitle("A Study");
            t.setBriefSummary(summary);
            return t;
        }

        private PatientDiagnosis metastatic() {
            PatientDiagnosis d = new PatientDiagnosis();
            d.setStage("Stage IV");
            return d;
        }

        /** Real trials from the corpus. Each is a genuine metastasis-directed study. */
        @ParameterizedTest
        @ValueSource(strings = {
                // NCT03808337 - the pattern bug that dropped this said "metastatic" only
                "SBRT delivered to all sites of disease in participants with 1-5 metastases",
                // NCT04563507 - a regimen matching this test's patient
                "Patients receiving letrozole+palbociclib are randomly assigned to also receive "
                        + "Stereotactic Body Radiation Therapy to each metastatic lesion",
                // NCT04158843
                "Radical local treatment versus palliative treatment for breast cancer patients "
                        + "with primary ipsilateral humerus or sternum oligometastasis",
                // NCT06246968
                "Participants with metastatic breast cancer receive pembrolizumab in combination "
                        + "with cryoablation",
                // NCT06055881
                "This study assesses if metastasis-directed radiation therapy can delay a change "
                        + "in systemic therapy"
        })
        void ablativeStrategyPasses(String summary) {
            assertThat(evaluator.treatmentGoalSignal(trial(summary)).outcome())
                    .isEqualTo(SignalOutcome.PASS);
        }

        /**
         * Every family-1 false positive in the corpus was a negation - the word appears while
         * being denied. One token separates "curative intent" from "not curative", which is the
         * same reason embeddings cannot read receptor polarity.
         */
        @ParameterizedTest
        @ValueSource(strings = {
                // NCT06682793
                "adults with recurrent unresectable, locally advanced, or metastatic "
                        + "(considered non-curative) solid tumors",
                // NCT07062965
                "Advanced cancer is a term often used to describe cancer that is unlikely to be cured",
                // NCT05601440
                "First line endocrine therapy improve clinical outcomes, but are not curative, "
                        + "and acquired resistance develops"
        })
        void negatedCureLanguageDoesNotFire(String summary) {
            assertThat(evaluator.treatmentGoalSignal(trial(summary)).outcome())
                    .isNotEqualTo(SignalOutcome.PASS);
            assertThat(evaluator.treatmentGoalSignal(trial(summary)).outcome())
                    .isNotEqualTo(SignalOutcome.UNKNOWN);
        }

        /**
         * A summary that says the disease is hard to cure and then states a curative aim must be
         * judged on the aim, not the background sentence that happens to come first.
         */
        @Test
        @DisplayName("a later unnegated cure word is found past an earlier negated one")
        void scansPastNegatedOccurrence() {
            String summary = "Metastatic breast cancer remains difficult to cure in most cases. "
                    + "This study treats with curative intent.";
            assertThat(evaluator.treatmentGoalSignal(trial(summary)).outcome())
                    .isEqualTo(SignalOutcome.UNKNOWN);
        }

        /** Cure language alone is a question, not an answer, so it must not report PASS. */
        @Test
        @DisplayName("cure language without ablative strategy reports unknown, never pass")
        void cureLanguageAloneIsUnknown() {
            var signal = evaluator.treatmentGoalSignal(trial(
                    "Local therapy with curative intent should be considered for selected "
                            + "patients with bone-only metastatic disease"));
            assertThat(signal.outcome()).isEqualTo(SignalOutcome.UNKNOWN);
            assertThat(signal.evidence()).isNotBlank();
        }

        /**
         * Response-endpoint vocabulary was measured and excluded: 232 trials say it and 5 of 5
         * hand-checked were describing how outcomes are measured, not what the trial aims at.
         */
        @ParameterizedTest
        @ValueSource(strings = {
                "Secondary endpoints are clinical benefit rate (complete response + partial "
                        + "response + stable disease)",
                "Primary end point 3-years invasive disease free survival",
                "patients achieving pathological complete response after neoadjuvant chemotherapy"
        })
        void responseEndpointVocabularyDoesNotFire(String summary) {
            assertThat(evaluator.treatmentGoalSignal(trial(summary)).outcome())
                    .isEqualTo(SignalOutcome.NOT_APPLICABLE);
        }

        @Test
        @DisplayName("a silent trial is not-applicable, never a concern")
        void silenceIsNotAConcern() {
            assertThat(evaluator.treatmentGoalSignal(trial(
                    "A study of drug X versus placebo in advanced solid tumours")).outcome())
                    .isEqualTo(SignalOutcome.NOT_APPLICABLE);
        }

        @Test
        @DisplayName("metastatic trials pass the stage check")
        void metastaticPasses() {
            assertThat(evaluator.diseaseStageSignal(
                    trial("A study in metastatic breast cancer"), metastatic()).outcome())
                    .isEqualTo(SignalOutcome.PASS);
        }

        @Test
        @DisplayName("adjuvant trials are a concern for a metastatic patient")
        void adjuvantIsAConcern() {
            assertThat(evaluator.diseaseStageSignal(
                    trial("Postoperative adjuvant therapy with T-DM1 for one year"),
                    metastatic()).outcome())
                    .isEqualTo(SignalOutcome.CONCERN);
        }

        /**
         * Without \\b these match inside "unresectable" and "inoperable", which mean the
         * opposite. A trial reading "recurrent unresectable ... metastatic" was vetoed as
         * early-stage on that substring during the corpus measurement.
         */
        @ParameterizedTest
        @ValueSource(strings = {
                "recurrent unresectable metastatic disease",
                "inoperable metastatic breast cancer"
        })
        void negativePrefixesDoNotReadAsEarlyStage(String summary) {
            assertThat(evaluator.diseaseStageSignal(trial(summary), metastatic()).outcome())
                    .isEqualTo(SignalOutcome.PASS);
        }

        @Test
        @DisplayName("a trial naming both stages is unknown rather than judged")
        void bothStagesIsUnknown() {
            assertThat(evaluator.diseaseStageSignal(
                    trial("adjuvant therapy in early-stage and metastatic breast cancer"),
                    metastatic()).outcome())
                    .isEqualTo(SignalOutcome.UNKNOWN);
        }

        @Test
        @DisplayName("stage is not compared when the record does not say the cancer has spread")
        void notApplicableWithoutMetastaticRecord() {
            PatientDiagnosis early = new PatientDiagnosis();
            early.setStage("Stage II");
            assertThat(evaluator.diseaseStageSignal(
                    trial("A study in metastatic breast cancer"), early).outcome())
                    .isEqualTo(SignalOutcome.NOT_APPLICABLE);
        }

        @Test
        @DisplayName("every flag carries the text that produced it")
        void flagsCarryEvidence() {
            assertThat(evaluator.treatmentGoalSignal(trial(
                    "SBRT to all sites of disease in 1-5 metastases")).evidence()).isNotBlank();
            assertThat(evaluator.diseaseStageSignal(
                    trial("Postoperative adjuvant therapy"), metastatic()).evidence()).isNotBlank();
        }
    }
}
