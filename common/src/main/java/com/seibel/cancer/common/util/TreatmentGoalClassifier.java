package com.seibel.cancer.common.util;

import com.seibel.cancer.common.enums.TreatmentGoal;

import java.util.regex.Pattern;

/**
 * Reads a trial's own words to decide what it appears to be trying to achieve.
 *
 * <p><b>Why this lives in {@code :common}.</b> Two callers need it and they are on opposite sides
 * of the module graph: {@code :datafetcher} stamps the value at normalization, and root's
 * matching layer reports it as a signal. {@code :datafetcher} cannot see root, so putting the
 * patterns anywhere else would mean two copies that drift — and this project has already been
 * bitten by a second copy of a parser disagreeing with the first.
 *
 * <p><b>Every pattern here was measured, not guessed.</b> Run across all 2,473 trials on
 * 2026-08-21; the numbers and the hand-checked samples are in
 * {@code .claude/matching/CURATIVE_STEP1_MEASUREMENT.md}. The measurement overturned the design
 * it was testing, which is the reason it was done first.
 *
 * <p>Read the title and summary, never the eligibility criteria. Criteria state who may enrol,
 * and routinely describe a patient's treatment history — "curative intent" appears there meaning
 * therapy someone already received, which inverts the meaning on exactly the phrases that matter
 * most.
 */
public final class TreatmentGoalClassifier {

    /**
     * Metastasis-directed and ablative strategy — the language that actually works.
     *
     * <p>Carried 26 of the 38 corpus survivors at near-perfect precision. It names something
     * being done to a metastasis, so unlike response vocabulary it cannot turn up in an endpoint
     * definition or a description of prior treatment.
     */
    private static final Pattern ABLATIVE_STRATEGY = Pattern.compile(
            "oligometasta\\w*|oligoprogress\\w*|metastasis[- ]directed|\\bSBRT\\b"
                    + "|stereotactic body|ablation|ablative|metastasectomy"
                    + "|total metastatic ablation|radical local",
            Pattern.CASE_INSENSITIVE);

    /**
     * Explicit cure language. A confirmer, never the primary test.
     *
     * <p>72 trials say one of these; only 12 reach the final signal on this alone, and that dozen
     * holds every false positive.
     *
     * <p>{@code eradicat*} is deliberately absent — its only corpus matches described axillary
     * disease "eradicated by neoadjuvant chemotherapy", which is early-stage and the opposite of
     * what is being looked for.
     */
    private static final Pattern EXPLICIT_CURE = Pattern.compile(
            "\\bcure\\b|\\bcured\\b|\\bcurable\\b|curative[- ]intent|\\bcurative\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * Negation immediately before a cure word, which inverts it.
     *
     * <p><b>Every false positive on cure language was a negation:</b> "considered non-curative",
     * "unlikely to be cured", "improve outcomes, but are not curative". The phrase and its denial
     * differ by one token — the same reason embedding similarity cannot read receptor polarity —
     * so the only way to tell them apart is to look left.
     */
    private static final Pattern CURE_NEGATED = Pattern.compile(
            "\\b(not|non|never|rarely|seldom|un\\w*|cannot|can't|incurable|no longer|without"
                    + "|difficult to|unlikely to be|hard to|fail\\w* to)\\b[\\s\\-]{0,4}$",
            Pattern.CASE_INSENSITIVE);

    /** Characters of context searched for a negation. Covers every observed case. */
    private static final int NEGATION_LOOKBEHIND = 40;

    private TreatmentGoalClassifier() {
    }

    /**
     * Classifies a trial from its own description.
     *
     * <p>Response-endpoint vocabulary — "complete response", "disease-free survival" — is
     * deliberately not read. 232 trials use it and five of five hand-checked were describing how
     * an outcome gets measured, not what the study is aiming at.
     *
     * @param titleAndSummary the trial's title, summary and description concatenated; null or
     *                        blank yields {@link TreatmentGoal#NOT_STATED}, since absence of text
     *                        is absence of evidence rather than evidence of absence
     */
    public static TreatmentGoal classify(String titleAndSummary) {
        if (titleAndSummary == null || titleAndSummary.isBlank()) {
            return TreatmentGoal.NOT_STATED;
        }
        String flat = titleAndSummary.replaceAll("\\s+", " ");

        if (ABLATIVE_STRATEGY.matcher(flat).find()) {
            return TreatmentGoal.ABLATIVE;
        }
        return firstUnnegatedCure(flat) != null
                ? TreatmentGoal.CURE_LANGUAGE
                : TreatmentGoal.NOT_STATED;
    }

    /**
     * The first cure word that is not being denied, with surrounding context, or null.
     *
     * <p>Also the evidence a reader is shown, so it returns the phrase rather than a boolean.
     *
     * <p>Scans every occurrence rather than stopping at the first: a summary opening with
     * "metastatic breast cancer remains difficult to cure" and stating a curative aim later would
     * otherwise be judged on its background sentence.
     */
    public static String firstUnnegatedCure(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String flat = text.replaceAll("\\s+", " ");
        var m = EXPLICIT_CURE.matcher(flat);
        while (m.find()) {
            String before = flat.substring(Math.max(0, m.start() - NEGATION_LOOKBEHIND), m.start());
            if (!CURE_NEGATED.matcher(before).find()) {
                int from = Math.max(0, m.start() - NEGATION_LOOKBEHIND);
                int to = Math.min(flat.length(), m.end() + 80);
                return flat.substring(from, to).strip();
            }
        }
        return null;
    }

    /** The ablative phrase that matched, with context, or null. Shown as evidence. */
    public static String firstAblativePhrase(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String flat = text.replaceAll("\\s+", " ");
        var m = ABLATIVE_STRATEGY.matcher(flat);
        if (!m.find()) {
            return null;
        }
        int from = Math.max(0, m.start() - 40);
        int to = Math.min(flat.length(), m.end() + 80);
        return flat.substring(from, to).strip();
    }
}
