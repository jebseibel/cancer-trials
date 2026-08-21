package com.seibel.cancer.service.matching;

import com.seibel.cancer.common.domain.Location;
import com.seibel.cancer.common.domain.PatientDiagnosis;
import com.seibel.cancer.common.domain.PatientPriorTreatment;
import com.seibel.cancer.common.domain.PatientVariant;
import com.seibel.cancer.common.domain.Trial;
import com.seibel.cancer.common.domain.matching.EligibilitySignal;
import com.seibel.cancer.common.domain.matching.SignalOutcome;
import com.seibel.cancer.common.enums.ReceptorStatus;
import com.seibel.cancer.common.enums.TreatmentStatus;
import com.seibel.cancer.common.enums.VariantStatus;
import com.seibel.cancer.common.util.TreatmentGoalClassifier;
import com.seibel.cancer.rag.chunk.EligibilityChunk;
import com.seibel.cancer.rag.chunk.EligibilityCriteriaChunker;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Tests a patient's structured record against what a trial's criteria text appears to require.
 *
 * <p><b>Deliberately keyword-based, and honest about it.</b> Semantic similarity is what
 * produced the failure this exists to fix: "HR-negative, HER2-negative" and "HR-positive,
 * HER2-negative" differ by one token, embeddings score them as near-identical, and a
 * triple-negative trial ranked first for an ER-positive patient. Retrieval is excellent at
 * finding candidate trials and blind to polarity; deterministic patterns are the opposite.
 * Each is used where it is strong — retrieval ranks, this decides.
 *
 * <p>Every pattern here can be wrong, so every method can return
 * {@link com.seibel.cancer.common.domain.matching.SignalOutcome#UNKNOWN}. Nothing this class
 * produces removes a trial from a list; the worst outcome is a demotion and a flag the reader
 * can check against the quoted evidence.
 *
 * <p><b>Pattern-writing rule that matters more than any individual pattern:</b> match the
 * requirement, not the topic. A trial that says "patients with HER2-positive disease are
 * excluded" mentions HER2-positive and requires HER2-negative. Patterns therefore anchor on
 * phrases that state a requirement, and {@link #sectionsOf} flips the reading when one appears
 * in an exclusion clause.
 *
 * <p><b>Where the inclusion/exclusion split comes from.</b> {@link EligibilityCriteriaChunker}
 * already solves this — it is the same parser that produces the {@code isExclusion} chunk
 * metadata the retrieval layer filters on, tuned against 50 real trials and later surveyed
 * across all 4,634. Reusing it means one parser to keep correct, and it means a criteria block
 * this evaluator cannot attribute to a section is the same block retrieval cannot attribute
 * either. Re-deriving the split from section headers here would be a second, worse copy that
 * drifts from the first.
 *
 * <p>When the parser cannot find headers at all it returns UNPARSED, and this class then reads
 * the block as inclusion-only. That is the pre-existing behaviour rather than a new guess: the
 * legacy "DISEASE CHARACTERISTICS:" records have no inclusion/exclusion split to recover, and
 * inventing one would flip polarity on text that never stated it.
 */
@Component
public class CriteriaSignalEvaluator {

    /**
     * Stateless and framework-free, so it is constructed directly rather than injected — it
     * carries only two tuning ints and has no Spring dependency of its own.
     */
    private final EligibilityCriteriaChunker chunker = new EligibilityCriteriaChunker();

    // Receptor requirement phrases. Written against the vocabulary CT.gov criteria actually
    // use - "triple negative", "TNBC", "hormone receptor positive", "HR+", "ER+" and so on.
    private static final Pattern TRIPLE_NEGATIVE = Pattern.compile(
            "\\btriple[\\s-]?negative\\b|\\bTNBC\\b", Pattern.CASE_INSENSITIVE);

    /**
     * <p>{@code positiv\w*} rather than {@code positive}: NCT05894239 writes "documenting
     * HER2-positivity", which the exact word missed entirely — so a trial requiring
     * HER2-positive disease reported four passes and zero concerns for a HER2-negative patient
     * and ranked 4th in the first live run of the endpoint.
     */
    private static final Pattern HER2_POSITIVE_REQUIRED = Pattern.compile(
            "\\bHER2[\\s-]?(positiv\\w*|\\+)|\\bHER2[\\s-]?amplified\\b|\\berbb2[\\s-]?amplified\\b",
            Pattern.CASE_INSENSITIVE);

    // The "-" alternative needs an explicit lookahead rather than \b. A hyphen followed by a
    // space is not a word boundary, so "HER2- metastatic" - the shorthand this branch exists
    // for - silently missed while "HER2-negative" matched via the word branch. The asymmetry
    // was invisible because the positive side does handle "+" shorthand correctly.
    private static final Pattern HER2_NEGATIVE_REQUIRED = Pattern.compile(
            "\\bHER2[\\s-]?negativ\\w*|\\bHER2\\s?-(?![\\w-])|\\bHER2[\\s-]?non[\\s-]?amplified\\b",
            Pattern.CASE_INSENSITIVE);

    // Spelled-out receptor names matter as much as the abbreviations: criteria text uses
    // "estrogen receptor positive" and "progesterone receptor positive" as often as "ER+", and
    // omitting them was a missed PASS on this patient's most important axis. "PgR" is the
    // pathology-report spelling of progesterone receptor. The optional parenthetical covers
    // "estrogen receptor (ER)-positive", which is standard in trial write-ups.
    private static final Pattern HORMONE_POSITIVE_REQUIRED = Pattern.compile(
            "\\b(HR|ER|PR|PgR|(o?estrogen|progesterone)([\\s-]receptor)?|hormone[\\s-]receptor)"
                    + "(\\s?\\([A-Za-z]{2,3}\\))?[\\s-]?(positive|\\+)",
            Pattern.CASE_INSENSITIVE);

    // Treatment-line phrases.
    private static final Pattern POST_CDK46 = Pattern.compile(
            "\\b(progress\\w*|refractory|resistan\\w*|relapsed?)\\b[^.]{0,60}\\bCDK\\s?4/?6\\b"
                    + "|\\bCDK\\s?4/?6\\b[^.]{0,60}\\b(progress\\w*|refractory|resistan\\w*)\\b"
                    + "|\\bpost[\\s-]?CDK\\s?4/?6\\b|\\bprior\\s+CDK\\s?4/?6\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * The drug class named at all, with no claim about when or to what effect.
     *
     * <p>Only safe to read inside an exclusion section, where naming a class is itself the bar
     * — "Prior treatment with a CDK4/6 inhibitor" under Exclusion needs no progression wording
     * to mean disqualifying. {@link #POST_CDK46} requires "prior" adjacent to "CDK", which
     * misses the far more common "prior treatment with a CDK4/6 inhibitor", "received a CDK4/6
     * inhibitor" and "any prior therapy with a CDK4/6 inhibitor".
     */
    private static final Pattern CDK46_MENTIONED = Pattern.compile(
            "\\bCDK\\s?4/?6\\b", Pattern.CASE_INSENSITIVE);

    /**
     * A naive/first-line requirement.
     *
     * <p>The "no prior ..." branch allows qualifying words between "prior" and the noun —
     * "no prior endocrine therapy" and "no prior systemic treatment" are the common shapes, and
     * requiring adjacency matched neither. Same adjacency trap as {@link #POST_CDK46}.
     *
     * <p>Only meaningful alongside {@link #RELEVANT_THERAPY_CLASS} in the same criterion; on its
     * own it says nothing about which treatment.
     */
    private static final Pattern TREATMENT_NAIVE = Pattern.compile(
            "\\btreatment[\\s-]?na[iï]ve\\b|\\bna[iï]ve\\s+to\\b"
                    + "|\\bno\\s+prior\\b[^.]{0,30}\\b(therapy|treatment|regimen)\\b"
                    + "|\\bfirst[\\s-]?line\\b|\\buntreated\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * The therapy classes a CDK4/6 history can actually speak to.
     *
     * <p>{@link #TREATMENT_NAIVE} alone says nothing about <em>which</em> treatment, and on its
     * own it fired on "no prior treatment for their DLBCL" and "Relapsed AML" — 550 concerns and
     * zero passes across the corpus, most of them other diseases. Requiring one of these names
     * in the same criterion is what makes a naive/prior reading about her regimen rather than
     * about any untreated illness.
     *
     * <p>Endocrine therapy belongs here alongside CDK4/6: the two are given together as
     * first-line treatment for HR+/HER2- disease — a common combination is a CDK4/6 inhibitor plus an aromatase inhibitor — so a
     * trial's endocrine-naive requirement bears on a patient's CDK4/6 status directly.
     */
    private static final Pattern RELEVANT_THERAPY_CLASS = Pattern.compile(
            "\\bCDK\\s?4/?6\\b|\\bpalbociclib\\b|\\bribociclib\\b|\\babemaciclib\\b"
                    + "|\\bendocrine\\s+therapy\\b|\\bhormonal\\s+therapy\\b|\\bhormone\\s+therapy\\b"
                    + "|\\baromatase\\s+inhibitor\\b|\\bletrozole\\b|\\banastrozole\\b"
                    + "|\\bexemestane\\b|\\btamoxifen\\b|\\bfulvestrant\\b",
            Pattern.CASE_INSENSITIVE);

    // PI3K pathway. Both the biomarker requirement and the prior-exposure limit.
    private static final Pattern PIK3CA_REQUIRED = Pattern.compile(
            "\\bPIK3CA\\b|\\bPI3K\\b|\\bAKT1\\b|\\bPTEN\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern PRIOR_PI3K_EXCLUDED = Pattern.compile(
            "\\bprior\\b[^.]{0,40}\\b(PI3K|AKT|mTOR)\\b[^.]{0,40}\\binhibitor",
            Pattern.CASE_INSENSITIVE);

    /**
     * Words that identify breast disease.
     *
     * <p>{@code mammary} catches the formal phrasing, and TNBC/DCIS/IDC are written as
     * abbreviations often enough to matter. Deliberately not "BC" — it collides with birth
     * control, blood count and B-cell in criteria text.
     */
    private static final Pattern BREAST_DISEASE = Pattern.compile(
            "\\bbreast\\b|\\bmammary\\b|\\bTNBC\\b|\\bDCIS\\b|\\bIDC\\b", Pattern.CASE_INSENSITIVE);

    /**
     * Other tumour types, for spotting a basket trial that merely lists breast among many.
     *
     * <p>Naming three or more of these alongside breast means the trial is pan-tumour rather
     * than breast-focused. Measured across the corpus: 48 of 1,370 breast-mentioning trials.
     */
    private static final Pattern OTHER_TUMOUR_TYPE = Pattern.compile(
            "\\b(lung|colorect\\w*|prostate|gastric|pancrea\\w*|melanoma|leukemi\\w*|lymphom\\w*"
                    + "|myelom\\w*|ovarian|renal|bladder|glioma\\w*|sarcoma\\w*|hepatocell\\w*"
                    + "|esophag\\w*|cervical|endometri\\w*|head and neck|NSCLC|SCLC)\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * Metastatic disease vocabulary.
     *
     * <p>{@code metasta(tic|sis|ses|tases)} as one stem rather than {@code metastatic} alone:
     * NCT03808337 — the clearest curative-intent trial in the corpus — says "1-5 metastases",
     * and an adjective-only pattern dropped it entirely while the distribution looked healthy.
     *
     * <p>Bare {@code advanced} is deliberately absent. "Locally advanced" is stage III, and
     * admitting it would let in exactly the early-stage trials {@link #EARLY_STAGE_DISEASE}
     * exists to catch.
     */
    private static final Pattern METASTATIC_DISEASE = Pattern.compile(
            "metasta(tic|sis|ses|tases)\\w*|stage IV|stage 4|\\bMBC\\b|\\bM1\\b|\\brecurrent\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * Early-stage scope, which disqualifies a trial from being a stage IV cure attempt.
     *
     * <p>Load-bearing: it removed 27 of 65 candidates (42%) in the corpus measurement, and the
     * removed ones are adjuvant and neoadjuvant studies — curative in intent, wrong disease
     * stage, and useless to a patient who is already metastatic.
     *
     * <p>⚠️ {@code \b} on {@code operable} and {@code resectable} is not decoration. Without it
     * they match inside "inoperable" and "unresectable", which mean the opposite, and a trial
     * reading "recurrent unresectable ... metastatic" was vetoed as early-stage on that
     * substring. Third occurrence of this class of bug in this file's history.
     */
    private static final Pattern EARLY_STAGE_DISEASE = Pattern.compile(
            "\\badjuvant\\b|neoadjuvant|early[- ]stage|\\bstage (0|I|II|III)\\b|\\boperable\\b"
                    + "|\\bresectable\\b|postoperative|preoperative|\\bDCIS\\b|\\bin situ\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * Assesses whether the trial is about breast cancer at all.
     *
     * <p><b>The gate the corpus sweep showed was missing.</b> Only 45.7% of this corpus is
     * breast — a mistaken first pull loaded 2,500 general-cancer trials — so the other signals
     * spend most of their effort on colorectal, AML and DLBCL studies, and reported 77-88%
     * NOT_APPLICABLE as a result. Without this, over half of any ranked list is trials for a
     * disease the patient does not have.
     *
     * <p><b>Read from the title and summary, not the criteria.</b> Criteria text mentions breast
     * in passing on pan-tumour studies — "solid tumors including breast" — so gating on it
     * admits exactly the trials this is meant to catch. Title-or-summary reproduces the known
     * 45.5% breast share of the corpus, which is the check that this reads the right fields.
     *
     * <p><b>A concern, never a removal</b>, per the no-verdicts rule. A basket trial enrolling
     * several tumour types can still be open to her, and an off-topic trial demoted to the
     * bottom of a list is recoverable where a deleted one is not.
     */
    public EligibilitySignal diseaseTypeSignal(Trial trial) {
        String name = "Disease type";
        String haystack = (nullToEmpty(trial.getBriefTitle()) + " "
                + nullToEmpty(trial.getOfficialTitle()) + " "
                + nullToEmpty(trial.getBriefSummary())).strip();
        if (haystack.isBlank()) {
            return EligibilitySignal.unknown(name,
                    "This trial has no title or summary recorded, so what it studies could not "
                            + "be checked.");
        }

        String breastMatch = firstMatch(haystack, BREAST_DISEASE);
        if (breastMatch == null) {
            return EligibilitySignal.concern(name,
                    "This trial does not appear to be about breast cancer. It may still be open "
                            + "to people with other diagnoses, but it is unlikely to be relevant.",
                    firstSentence(haystack));
        }

        // A trial naming several tumour types is a basket study: breast is one arm among many,
        // so it is worth reading differently from a breast-focused trial.
        long otherTypes = OTHER_TUMOUR_TYPE.matcher(haystack).results()
                .map(r -> r.group().toLowerCase())
                .distinct()
                .count();
        if (otherTypes >= 3) {
            return EligibilitySignal.unknown(name,
                    "This trial covers several cancer types rather than breast cancer "
                            + "specifically. Whether it is currently enrolling people with breast "
                            + "cancer is worth asking.");
        }

        return EligibilitySignal.pass(name, "This trial is about breast cancer.", breastMatch);
    }

    /**
     * Assesses what the trial is trying to <b>achieve</b>, as opposed to who may enrol.
     *
     * <p><b>The first signal about goal rather than eligibility.</b> Every other signal here
     * answers "does she qualify?"; none asks "and if she got in, what is this trial trying to do
     * for her?". In metastatic breast cancer the overwhelming majority of trials test disease
     * control — a longer interval before the next line — which is worth having and is not what
     * was asked for.
     *
     * <p><b>Read from title and summary, never criteria.</b> Criteria state who may enrol, and
     * routinely describe a patient's past treatment: "curative intent" appears there meaning the
     * therapy someone already received. Reading intent from criteria inverts the meaning on
     * exactly the phrases that matter most. {@link #diseaseTypeSignal} set this precedent for
     * the same reason.
     *
     * <p><b>Ablative language leads, cure language confirms.</b> Measured across 2,473 trials:
     * 26 of 38 survivors come from {@link TreatmentGoalClassifier} at near-perfect precision, while
     * the 12 that arrive on cure language alone carry every false positive. Response-endpoint
     * vocabulary — "complete response", "disease-free survival" — was measured and
     * <b>excluded</b>: 232 trials say it, and 5 of 5 hand-checked were reporting how outcomes are
     * measured, not what the trial was trying to do.
     *
     * <p>PASS is deliberately reserved for ablative strategy. Cure language alone returns UNKNOWN
     * rather than PASS — it is a question worth raising, not an answer.
     */
    public EligibilitySignal treatmentGoalSignal(Trial trial) {
        String name = "Treatment goal";
        String haystack = (nullToEmpty(trial.getBriefTitle()) + " "
                + nullToEmpty(trial.getOfficialTitle()) + " "
                + nullToEmpty(trial.getBriefSummary()) + " "
                + nullToEmpty(trial.getDetailedDescription())).strip();

        if (haystack.isBlank()) {
            return EligibilitySignal.unknown(name,
                    "This trial has no title or summary recorded, so what it is trying to achieve "
                            + "could not be checked.");
        }

        String ablative = TreatmentGoalClassifier.firstAblativePhrase(haystack);
        if (ablative != null) {
            return EligibilitySignal.pass(name,
                    "This trial treats the individual sites of spread rather than only slowing "
                            + "the disease. That is the approach used when the aim is long-term "
                            + "control, and it is worth asking the care team about.",
                    ablative);
        }

        String cure = TreatmentGoalClassifier.firstUnnegatedCure(haystack);
        if (cure != null) {
            return new EligibilitySignal(name, SignalOutcome.UNKNOWN,
                    "This trial's description uses the language of cure or long-term remission. "
                            + "Whether that is this study's aim, or background about the disease, "
                            + "is worth reading the quoted text to judge.",
                    cure);
        }

        // Silence, not a concern. Most trials test disease control and that is legitimate; a
        // trial that says nothing about cure has not failed a test, it simply was not asked.
        return new EligibilitySignal(name, SignalOutcome.NOT_APPLICABLE,
                "This trial does not describe treating the sites of spread directly or aiming at "
                        + "long-term remission.", null);
    }

    /**
     * Assesses whether the trial is for the stage of disease the patient actually has.
     *
     * <p><b>Separate from {@link #treatmentGoalSignal} on purpose.</b> "Trying to cure" and "for
     * stage IV disease" are different questions that can disagree, and a combined signal
     * reporting CONCERN gives no way to tell whether a trial is the wrong stage or merely the
     * wrong ambition. Those lead to different conversations with an oncologist.
     *
     * <p>A curative trial for early-stage disease is a correct match on intent and useless to a
     * metastatic patient — which is why the corpus measurement had to veto 42% of its candidates
     * as adjuvant or neoadjuvant.
     *
     * <p>Only meaningful for a patient who is metastatic. For anyone else this returns
     * NOT_APPLICABLE rather than guessing what stage-matching should mean.
     */
    public EligibilitySignal diseaseStageSignal(Trial trial, PatientDiagnosis diagnosis) {
        String name = "Disease stage";
        String haystack = (nullToEmpty(trial.getBriefTitle()) + " "
                + nullToEmpty(trial.getOfficialTitle()) + " "
                + nullToEmpty(trial.getBriefSummary()) + " "
                + nullToEmpty(trial.getDetailedDescription())).strip();

        if (haystack.isBlank()) {
            return EligibilitySignal.unknown(name,
                    "This trial has no title or summary recorded, so the stage of disease it "
                            + "studies could not be checked.");
        }
        if (diagnosis == null || !isMetastatic(diagnosis)) {
            return new EligibilitySignal(name, SignalOutcome.NOT_APPLICABLE,
                    "The record on file does not say the cancer has spread, so trials were not "
                            + "compared on stage.", null);
        }

        String metastatic = firstMatch(haystack, METASTATIC_DISEASE);
        String earlyStage = firstMatch(haystack, EARLY_STAGE_DISEASE);

        // Both present is common and genuinely ambiguous - a trial may enrol early-stage
        // patients while discussing metastatic disease in its rationale. Saying so is more
        // honest than picking a side.
        if (metastatic != null && earlyStage != null) {
            return new EligibilitySignal(name, SignalOutcome.UNKNOWN,
                    "This trial mentions both cancer that has spread and earlier-stage disease. "
                            + "Which group it is actually enrolling is worth checking.",
                    earlyStage);
        }
        if (metastatic != null) {
            return EligibilitySignal.pass(name,
                    "This trial is for cancer that has spread, which matches the record on file.",
                    metastatic);
        }
        if (earlyStage != null) {
            return EligibilitySignal.concern(name,
                    "This trial appears to be for earlier-stage disease, before it has spread. "
                            + "That is a different situation from the record on file.",
                    earlyStage);
        }
        return EligibilitySignal.unknown(name,
                "This trial's description does not say what stage of disease it studies.");
    }

    /**
     * Whether the record says the disease has spread.
     *
     * <p>Reads the free-text stage field, since the schema stores stage as text rather than an
     * enum. Accepts the several ways a real record writes it - "Stage IV", "IV", "cM1",
     * "metastatic" - because the value came from a pathology report, not a dropdown.
     */
    private boolean isMetastatic(PatientDiagnosis diagnosis) {
        String stage = nullToEmpty(diagnosis.getStage());
        return METASTATIC_DISEASE.matcher(stage).find()
                || stage.trim().equalsIgnoreCase("IV")
                || stage.trim().equals("4");
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /** Enough of the text for a reader to see what the trial is actually about. */
    private String firstSentence(String text) {
        String flat = text.replaceAll("\\s+", " ").strip();
        int stop = flat.indexOf(". ");
        String s = stop > 0 ? flat.substring(0, stop + 1) : flat;
        return s.length() <= 160 ? s : s.substring(0, 160) + "...";
    }

    /**
     * Assesses HER2 and hormone-receptor polarity.
     *
     * <p>The signal this whole tier exists for. Receptor status gates 36% of this corpus on
     * HER2 and 28% on ER/PR, and it is precisely what retrieval cannot see.
     */
    public EligibilitySignal receptorSignal(Trial trial, PatientDiagnosis diagnosis) {
        String name = "Receptor status";
        String criteria = trial.getEligibilityCriteria();
        if (criteria == null || criteria.isBlank()) {
            return EligibilitySignal.notApplicable(name);
        }

        Sections sections = sectionsOf(criteria);
        if (sections.isEmpty()) {
            return EligibilitySignal.notApplicable(name);
        }

        ReceptorStatus er = ReceptorStatus.fromValue(diagnosis.getErStatus());
        ReceptorStatus pr = ReceptorStatus.fromValue(diagnosis.getPrStatus());
        ReceptorStatus her2 = ReceptorStatus.fromValue(diagnosis.getHer2Status());

        boolean hormonePositive = er.isPositive() || pr.isPositive();

        // Triple-negative first: it is the most specific claim a trial can make, and it is the
        // one that produced the original mis-ranking.
        //
        // Read from the inclusion side only. Under Exclusion the same phrase means the trial
        // rules triple-negative disease OUT, which for a hormone-positive patient is the
        // opposite of a concern - and flagging it would demote a trial she actually fits.
        String tnbcRequired = firstMatch(sections.inclusion(), TRIPLE_NEGATIVE);
        if (tnbcRequired != null && hormonePositive) {
            return EligibilitySignal.concern(name,
                    "This trial appears to be for triple-negative breast cancer. The record says "
                            + "hormone receptor positive, which is a different disease subtype. "
                            + "Worth confirming with the study team.",
                    tnbcRequired);
        }

        // HER2 polarity, likewise read per section. A trial whose Exclusion list says
        // "HER2-positive disease" requires HER2-NEGATIVE disease - it was the single largest
        // source of false concerns before the split existed.
        // Per criterion, not per section. A trial can name HER2-positivity as its requirement
        // and HER2-negative somewhere else entirely - NCT05894239 requires "documenting
        // HER2-positivity" and mentions HER2-negative in an unrelated inclusion line. Read
        // across the whole section, the second suppressed the first and a HER2-positive trial
        // reported four passes and zero concerns for a HER2-negative patient, ranking 4th.
        String her2PosRequired = firstCriterionRequiring(
                sections.inclusionCriteria(), HER2_POSITIVE_REQUIRED, HER2_NEGATIVE_REQUIRED);
        boolean her2PosExcluded = matches(sections.exclusion(), HER2_POSITIVE_REQUIRED);
        boolean her2NegRequired = matches(sections.inclusion(), HER2_NEGATIVE_REQUIRED);

        if (her2PosRequired != null && her2.isNegative()) {
            return EligibilitySignal.concern(name,
                    "This trial appears to require HER2-positive disease. The record says HER2 "
                            + "negative. Worth confirming with the study team.",
                    her2PosRequired);
        }

        if (her2.isUnresolved() || (er.isUnresolved() && pr.isUnresolved())) {
            return EligibilitySignal.unknown(name,
                    "Receptor status is not fully recorded, so this could not be checked. "
                            + "Fill in ER, PR and HER2 on the Diagnosis tab to enable it.");
        }

        String hormoneMatch = firstMatch(sections.inclusion(), HORMONE_POSITIVE_REQUIRED);
        if (hormoneMatch != null && hormonePositive) {
            return EligibilitySignal.pass(name,
                    "This trial mentions hormone-receptor-positive disease, which matches the record.",
                    hormoneMatch);
        }
        if (her2NegRequired && her2.isNegative()) {
            return EligibilitySignal.pass(name,
                    "This trial mentions HER2-negative disease, which matches the record.",
                    firstMatch(sections.inclusion(), HER2_NEGATIVE_REQUIRED));
        }
        // Excluding HER2-positive disease is how many trials state a HER2-negative
        // requirement, so it is a match for this patient rather than a silent no-op.
        if (her2PosExcluded && her2.isNegative()) {
            return EligibilitySignal.pass(name,
                    "This trial excludes HER2-positive disease, and the record says HER2 negative.",
                    firstMatch(sections.exclusion(), HER2_POSITIVE_REQUIRED));
        }
        // Same reading for triple-negative: ruling it out is a fit for hormone-positive disease.
        String tnbcExcluded = firstMatch(sections.exclusion(), TRIPLE_NEGATIVE);
        if (tnbcExcluded != null && hormonePositive) {
            return EligibilitySignal.pass(name,
                    "This trial excludes triple-negative disease, and the record says hormone "
                            + "receptor positive.",
                    tnbcExcluded);
        }

        return EligibilitySignal.notApplicable(name);
    }

    /**
     * Assesses treatment line against CDK4/6 history.
     *
     * <p>The distinction a boolean destroys: on a CDK4/6 inhibitor now and not progressed is
     * neither treatment-naive nor post-progression, and trials exist for both of those and not
     * for the middle.
     */
    public EligibilitySignal treatmentLineSignal(Trial trial, PatientPriorTreatment treatment) {
        String name = "Treatment history";
        String criteria = trial.getEligibilityCriteria();
        if (criteria == null || criteria.isBlank() || treatment == null) {
            return EligibilitySignal.notApplicable(name);
        }

        Sections sections = sectionsOf(criteria);
        if (sections.isEmpty()) {
            return EligibilitySignal.notApplicable(name);
        }

        TreatmentStatus cdk46 = TreatmentStatus.fromValue(treatment.getCdk46Status());

        // Prior CDK4/6 exposure named under Exclusion is a bar, not a requirement - the exact
        // inverse of the inclusion reading below. A patient currently on abemaciclib is
        // excluded by it, which is a concern worth raising rather than the PASS the
        // section-blind version produced.
        String priorCdkExcluded = firstCriterionMatching(
                sections.exclusionCriteria(), CDK46_MENTIONED, CDK46_MENTIONED);
        if (priorCdkExcluded != null && cdk46.hasReceived()) {
            return EligibilitySignal.concern(name,
                    "This trial appears to exclude people who have already had a CDK4/6 "
                            + "inhibitor, and the record shows one has been taken.",
                    priorCdkExcluded);
        }

        String postCdkMatch = firstMatch(sections.inclusion(), POST_CDK46);
        if (postCdkMatch != null) {
            if (cdk46.hasProgressedOn()) {
                return EligibilitySignal.pass(name,
                        "This trial is for people whose CDK4/6 inhibitor stopped working, which "
                                + "matches the record.",
                        postCdkMatch);
            }
            if (cdk46 == TreatmentStatus.CURRENT) {
                return EligibilitySignal.concern(name,
                        "This trial appears to be for people who have already progressed on a "
                                + "CDK4/6 inhibitor. The record says one is being taken now without "
                                + "progression, so it may be for a later stage of treatment.",
                        postCdkMatch);
            }
            if (cdk46.isNaive()) {
                return EligibilitySignal.concern(name,
                        "This trial appears to require prior CDK4/6 treatment, which the record "
                                + "does not show.",
                        postCdkMatch);
            }
            return EligibilitySignal.unknown(name,
                    "This trial mentions prior CDK4/6 treatment, but the record does not say "
                            + "enough to compare. Fill in the Prior Treatment tab to enable this.");
        }

        // Both patterns must appear in the SAME criterion. Read across a whole section, a
        // "first-line" line about one disease and a CDK4/6 line about another have no
        // relationship, and treating them as one statement is what produced 550 concerns
        // against 0 passes on the corpus.
        String naiveMatch = firstCriterionMatching(
                sections.inclusionCriteria(), TREATMENT_NAIVE, RELEVANT_THERAPY_CLASS);
        if (naiveMatch != null) {
            if (cdk46.isNaive()) {
                return EligibilitySignal.pass(name,
                        "This trial is for people not yet treated with this drug class, which "
                                + "matches the record.",
                        naiveMatch);
            }
            if (cdk46.hasReceived()) {
                return EligibilitySignal.concern(name,
                        "This trial appears to be for people who have not had this kind of "
                                + "treatment before. The record shows a CDK4/6 inhibitor has "
                                + "been taken.",
                        naiveMatch);
            }
        }

        return EligibilitySignal.notApplicable(name);
    }

    /**
     * Assesses the PI3K/AKT/mTOR pathway — a genuine opportunity rather than a filter.
     *
     * <p>A PIK3CA-mutant, PI3K-inhibitor-naive patient is an inclusion criterion for a whole
     * class of trials, so this signal exists as much to surface fits as to raise concerns.
     */
    public EligibilitySignal pi3kSignal(Trial trial, PatientVariant variant,
                                        PatientPriorTreatment treatment) {
        String name = "PI3K pathway";
        String criteria = trial.getEligibilityCriteria();
        if (criteria == null || criteria.isBlank() || variant == null) {
            return EligibilitySignal.notApplicable(name);
        }

        Sections sections = sectionsOf(criteria);
        if (sections.isEmpty()) {
            return EligibilitySignal.notApplicable(name);
        }

        VariantStatus pik3ca = VariantStatus.fromValue(variant.getPik3caStatus());

        // A prior-PI3K exclusion is worth flagging separately, because a PIK3CA-mutant patient
        // who has already had a PI3K inhibitor is excluded from trials she otherwise fits.
        //
        // Two ways a trial states it: the drug class named anywhere in the Exclusion section,
        // or the "prior ... inhibitor" phrasing in a block with no parsed sections at all. The
        // section test is the reliable one; PRIOR_PI3K_EXCLUDED remains as the fallback for
        // UNPARSED text, where inclusion carries the whole block and word order is all there is.
        String priorPi3kMatch = firstMatch(sections.exclusion(), PIK3CA_REQUIRED);
        if (priorPi3kMatch == null) {
            priorPi3kMatch = firstMatch(sections.inclusion(), PRIOR_PI3K_EXCLUDED);
        }
        if (priorPi3kMatch != null && treatment != null) {
            TreatmentStatus pi3k = TreatmentStatus.fromValue(treatment.getPi3kAktMtorStatus());
            if (pi3k.hasReceived()) {
                return EligibilitySignal.concern(name,
                        "This trial appears to exclude people who have already had a PI3K, AKT or "
                                + "mTOR inhibitor, and the record shows one has been taken.",
                        priorPi3kMatch);
            }
        }

        // The pathway requirement itself is an inclusion reading: a trial that merely excludes
        // PI3K-treated patients is not a PI3K trial, and reporting it as one would claim a
        // biomarker opportunity the trial never offered.
        String pathwayMatch = firstMatch(sections.inclusion(), PIK3CA_REQUIRED);
        if (pathwayMatch == null) {
            return EligibilitySignal.notApplicable(name);
        }

        if (pik3ca.isDetected()) {
            return EligibilitySignal.pass(name,
                    "This trial involves the PI3K pathway and the record shows a PIK3CA mutation, "
                            + "which is often an entry requirement for these studies.",
                    pathwayMatch);
        }
        if (pik3ca.isRuledOut()) {
            return EligibilitySignal.concern(name,
                    "This trial involves the PI3K pathway. The record shows PIK3CA was tested and "
                            + "not found, which may be an entry requirement.",
                    pathwayMatch);
        }
        return EligibilitySignal.unknown(name,
                "This trial involves the PI3K pathway, but PIK3CA is not recorded as tested. "
                        + "Worth asking whether testing is required.");
    }

    /**
     * Assesses whether the trial recruits anywhere in the United States.
     *
     * <p>Geography was not part of matching at all, and the top-ranked trial from the first
     * real search had exactly one site outside the US. Distance is not modelled: the stated
     * constraint is willingness to travel anywhere in the USA, so the question is presence in
     * the country, not proximity.
     */
    public EligibilitySignal locationSignal(List<Location> locations) {
        String name = "Location";
        if (locations == null || locations.isEmpty()) {
            return EligibilitySignal.unknown(name,
                    "No study locations are recorded for this trial, so where it runs could not "
                            + "be checked.");
        }

        List<String> usSites = siteLabels(locations, true);

        if (!usSites.isEmpty()) {
            // Eight rather than three: this is a travel decision, and "and 9 more" hides
            // exactly the city that might be an hour away. The reader is choosing whether a
            // trial is reachable, so the list has to be long enough to answer that.
            int shown = Math.min(usSites.size(), 8);
            String sample = usSites.size() <= shown
                    ? String.join("; ", usSites)
                    : String.join("; ", usSites.subList(0, shown))
                            + " and " + (usSites.size() - shown) + " more";
            return EligibilitySignal.pass(name,
                    "This trial has " + usSites.size() + " location"
                            + (usSites.size() == 1 ? "" : "s") + " in the United States: " + sample,
                    sample);
        }

        String countries = siteLabels(locations, false).stream()
                .limit(3)
                .reduce((a, b) -> a + "; " + b)
                .orElse("not stated");

        return EligibilitySignal.concern(name,
                "This trial does not appear to have a United States location. Travel would be "
                        + "international, which is worth confirming before ruling it out.",
                countries);
    }

    /**
     * Distinct site labels, for display on a result card.
     *
     * @param unitedStatesOnly true for "City, State" of US sites; false for the countries the
     *                         trial runs in, so an international-only trial can say where
     *                         rather than appearing to have no locations at all
     */
    public List<String> siteLabels(List<Location> locations, boolean unitedStatesOnly) {
        if (locations == null || locations.isEmpty()) {
            return List.of();
        }
        if (!unitedStatesOnly) {
            return locations.stream()
                    .map(Location::getCountry)
                    .filter(c -> c != null && !c.isBlank())
                    .map(String::trim)
                    .distinct()
                    .toList();
        }
        return locations.stream()
                .filter(l -> isUnitedStates(l.getCountry()))
                .map(this::cityLabel)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .toList();
    }

    /** "City, State", or whichever half is recorded. */
    private String cityLabel(Location l) {
        boolean hasCity = l.getCity() != null && !l.getCity().isBlank();
        boolean hasState = l.getState() != null && !l.getState().isBlank();
        if (hasCity && hasState) {
            return l.getCity().trim() + ", " + l.getState().trim();
        }
        return hasCity ? l.getCity().trim() : (hasState ? l.getState().trim() : null);
    }

    /** Country strings vary across CT.gov records; match the ones that actually appear. */
    private boolean isUnitedStates(String country) {
        if (country == null || country.isBlank()) {
            return false;
        }
        String c = country.trim().toUpperCase();
        return c.equals("UNITED STATES") || c.equals("USA") || c.equals("US")
                || c.equals("UNITED STATES OF AMERICA");
    }

    /**
     * A trial's criteria text split into what it requires and what it rules out.
     *
     * <p>The distinction the whole class turns on. "Triple-negative breast cancer" under
     * Inclusion means the trial is for triple-negative disease; the identical phrase under
     * Exclusion means the trial is for everyone else, which for this patient is a fit rather
     * than a concern.
     *
     * @param inclusion text from inclusion sections, plus any UNPARSED block
     * @param exclusion text from exclusion sections; blank when the trial states none
     */
    private record Sections(String inclusion, String exclusion,
                            List<String> inclusionCriteria, List<String> exclusionCriteria) {

        boolean isEmpty() {
            return inclusion.isBlank() && exclusion.isBlank();
        }
    }

    /**
     * The first criterion in which both patterns appear, or null.
     *
     * <p><b>Why per-criterion rather than per-section.</b> Section text is every criterion
     * concatenated, so two patterns "co-occurring" there can be lines apart and about different
     * things entirely. Measured against the corpus on 2026-08-11: reading
     * {@link #TREATMENT_NAIVE} against whole sections produced 550 concerns and zero passes,
     * and the sampled flags were largely trials in other diseases — "no prior treatment for
     * their DLBCL", "Relapsed AML", "Untreated with anti-tumor therapy for rectal cancer". The
     * signal claimed to compare her CDK4/6 history and was in fact firing on any mention of any
     * untreated disease.
     *
     * <p>One criterion is the unit where co-occurrence actually implies a relationship, and
     * {@link EligibilityCriteriaChunker} already splits on exactly that boundary — including
     * prefixing nested items with their parent line, so "must be first-line" under a
     * "CDK4/6 inhibitor therapy:" heading still resolves.
     */
    /**
     * The first criterion matching {@code required} without also matching {@code contradicting}.
     *
     * <p>For opposite-polarity patterns, where a criterion naming both is stating a comparison
     * rather than a requirement ("HER2-negative or HER2-positive by local testing") and should
     * not be read as demanding either. Scoping that judgement to one criterion is the point: a
     * contradiction elsewhere in the section is a different statement about a different thing.
     */
    private String firstCriterionRequiring(List<String> criteria, Pattern required,
                                           Pattern contradicting) {
        for (String criterion : criteria) {
            if (required.matcher(criterion).find() && !contradicting.matcher(criterion).find()) {
                return firstMatch(criterion, required);
            }
        }
        return null;
    }

    private String firstCriterionMatching(List<String> criteria, Pattern a, Pattern b) {
        for (String criterion : criteria) {
            if (a.matcher(criterion).find() && b.matcher(criterion).find()) {
                return firstMatch(criterion, a);
            }
        }
        return null;
    }

    /**
     * Splits criteria into inclusion and exclusion text using the retrieval layer's parser.
     *
     * <p>UNPARSED chunks join the inclusion side. They are whole un-split blocks from records
     * that never stated a division, so treating them as exclusions would invert the reading of
     * every phrase in them.
     */
    private Sections sectionsOf(String criteria) {
        StringBuilder inclusion = new StringBuilder();
        StringBuilder exclusion = new StringBuilder();
        List<String> inclusionCriteria = new ArrayList<>();
        List<String> exclusionCriteria = new ArrayList<>();

        for (EligibilityChunk chunk : chunker.chunk(criteria)) {
            boolean isExclusion = chunk.type() == EligibilityChunk.ChunkType.EXCLUSION;
            StringBuilder target = isExclusion ? exclusion : inclusion;
            if (!target.isEmpty()) {
                // Newline, not a space: patterns bound context with [^.]{0,60}, and joining
                // two criteria on one line would let a match run across the boundary between
                // them and quote evidence that spans two unrelated requirements.
                target.append('\n');
            }
            target.append(chunk.text());
            (isExclusion ? exclusionCriteria : inclusionCriteria).add(chunk.text());
        }
        return new Sections(inclusion.toString(), exclusion.toString(),
                inclusionCriteria, exclusionCriteria);
    }

    /**
     * Returns the matched phrase with a little surrounding context, or null.
     *
     * <p>Context matters for a reader: the bare token "HER2-positive" is less useful than the
     * clause it sits in, which is what lets someone judge whether the flag is fair.
     */
    private String firstMatch(String text, Pattern pattern) {
        if (text == null || text.isBlank()) {
            return null;
        }
        var m = pattern.matcher(text);
        if (!m.find()) {
            return null;
        }
        int start = Math.max(0, m.start() - 40);
        int end = Math.min(text.length(), m.end() + 40);
        String snippet = text.substring(start, end).replaceAll("\\s+", " ").trim();
        return (start > 0 ? "..." : "") + snippet + (end < text.length() ? "..." : "");
    }

    /** True when the pattern appears anywhere in the text. Null-safe. */
    private boolean matches(String text, Pattern pattern) {
        return text != null && !text.isBlank() && pattern.matcher(text).find();
    }
}
