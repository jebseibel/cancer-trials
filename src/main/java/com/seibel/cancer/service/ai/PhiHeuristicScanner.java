package com.seibel.cancer.service.ai;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A local, offline gate against identifying detail in a document a user is about to hand to
 * the AI intake feature.
 *
 * <p><b>This is not the {@code TrialDiagnosisMatchService} allowlist.</b> That service reads
 * the app's own already-structured fields and sends only the ones an explicit allowlist names.
 * This class instead reads raw, unstructured prose a user pasted in - there is no field list to
 * allow, so the control has to be a scan of the content itself. It never calls anywhere off the
 * machine and needs no configuration to run.
 *
 * <p><b>Biased to over-flag, deliberately.</b> A false positive costs the user a rewrite. A
 * false negative would mean identifying detail reached the model. Every heuristic below is
 * written to accept that trade, and there is no scoring or threshold - a single hit on any
 * category is enough to reject the whole document.
 *
 * <p><b>Findings never carry the matched text.</b> {@link PhiScanResult#reasons()} is category
 * labels only, so a log line or an error message built from a scan result cannot itself repeat
 * whatever it caught.
 */
@Component
public class PhiHeuristicScanner {

    private static final Pattern SSN_DASHED = Pattern.compile("\\b\\d{3}-\\d{2}-\\d{4}\\b");
    private static final Pattern SSN_LABEL = Pattern.compile(
            "(?i)\\b(ssn|social security)\\b");
    private static final Pattern SSN_BARE = Pattern.compile("\\b\\d{9}\\b");

    private static final Pattern MRN = Pattern.compile(
            "(?i)\\b(MRN|medical record (number|no\\.?)|patient (id|number))\\b\\s*[:#]?\\s*\\S+");

    private static final Pattern DOB_LABEL = Pattern.compile(
            "(?i)\\b(DOB|D\\.O\\.B\\.?|date of birth|birth ?date)\\b");
    private static final Pattern DATE_TOKEN = Pattern.compile(
            "\\b(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{4}-\\d{2}-\\d{2}"
                    + "|(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)[a-z]*\\.?\\s+\\d{1,2}"
                    + "(,?\\s+\\d{4})?)\\b",
            Pattern.CASE_INSENSITIVE);
    /** Catches a birth date stated in prose with no "DOB"-style label at all, e.g. "born on
     * 3/14/1965" or "date of birth: March 14, 1965" phrased conversationally. Unlike
     * {@link #matchesLabeledDob}, the trigger word itself ("born") is the anchor, so this does
     * not require a colon or header formatting. */
    private static final Pattern UNLABELED_BIRTH_PHRASE = Pattern.compile(
            "(?i)\\bborn\\b\\s*(on|in)?\\s*:?\\s*(?=.{0,20}?"
                    + "(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}|\\d{4}-\\d{2}-\\d{2}"
                    + "|(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Sept|Oct|Nov|Dec)))");

    private static final Pattern PHONE = Pattern.compile(
            "\\b(\\+?1[-.\\s]?)?\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4}\\b");

    private static final Pattern EMAIL = Pattern.compile(
            "\\b[\\w.+-]+@[\\w-]+\\.[\\w.-]+\\b");

    private static final Pattern STREET_SUFFIX = Pattern.compile(
            "(?i)\\b\\d{1,6}\\s+[A-Za-z0-9.'\\s]{2,40}\\b"
                    + "(Street|St\\.?|Avenue|Ave\\.?|Boulevard|Blvd\\.?|Road|Rd\\.?|Drive|Dr\\.?"
                    + "|Lane|Ln\\.?|Way|Court|Ct\\.?|Circle|Cir\\.?|Terrace|Ter\\.?|Place|Pl\\.?"
                    + "|Trail|Trl\\.?|Parkway|Pkwy\\.?|Plaza|Plz\\.?)\\b");
    private static final Pattern ZIP_STATE = Pattern.compile(
            "\\b[A-Z]{2}\\s+\\d{5}(-\\d{4})?\\b");
    private static final Pattern ADDRESS_LABEL = Pattern.compile("(?i)\\baddress\\b\\s*[:#]?");

    private static final Pattern NAME_HEADER_LABEL = Pattern.compile(
            "(?i)\\b(patient name|name|pt\\.?|mr\\.?|mrs\\.?|ms\\.?|dr\\.?)\\b\\s*[:#]\\s*"
                    + "\\p{Lu}[\\p{Ll}'-]+(\\s+\\p{Lu}[\\p{Ll}'-]+){0,2}");
    private static final Pattern LABELED_NAME_PAIR = Pattern.compile(
            "[A-Za-z ]+:\\s*\\p{Lu}[\\p{Ll}'-]+\\s+\\p{Lu}[\\p{Ll}'-]+");
    /** Catches "Patient Jane Doe ..." in prose, with no colon and no "Name" label - only
     * "Patient Name:" trips {@link #NAME_HEADER_LABEL}. Requires the word directly followed by
     * two capitalized tokens so it can't fire on "Patient presented with ..." (lower-case) or a
     * single capitalized word (a lone surname, or a sentence-initial capital by itself). */
    private static final Pattern UNLABELED_PATIENT_NAME = Pattern.compile(
            "\\b(Patient|Pt\\.?)\\s+\\p{Lu}[\\p{Ll}'-]+\\s+\\p{Lu}[\\p{Ll}'-]+\\b");
    /** Catches a title used conversationally with no colon/"#" after it, e.g. "Ms Jane Doe was
     * evaluated..." - {@link #NAME_HEADER_LABEL} requires punctuation right after the title, and
     * {@link #UNLABELED_PATIENT_NAME} only recognizes "Patient"/"Pt". Deliberately excludes
     * "Name"/"Patient Name" here since those are already covered, and excludes bare "Dr" since
     * {@link #CLINICIAN_NAME} already covers that with its own single-name form. */
    private static final Pattern UNPUNCTUATED_TITLE_NAME = Pattern.compile(
            "\\b(Ms|Mr|Mrs)\\.?\\s+\\p{Lu}[\\p{Ll}'-]+(\\s+\\p{Lu}[\\p{Ll}'-]+)?\\b");
    /** Catches a name set off by commas in apposition, e.g. "The patient, Jane Doe, was
     * evaluated today." Neither of the other name rules matches here since the triggering word
     * ("patient") is lower-case and separated from the name by a comma, not adjacent to it. */
    private static final Pattern APPOSITION_NAME = Pattern.compile(
            "(?i)\\b(the )?patient,\\s*\\p{Lu}[\\p{Ll}'-]+\\s+\\p{Lu}[\\p{Ll}'-]+\\s*,");

    private static final Pattern CLINICIAN_NAME = Pattern.compile(
            "\\bDr\\.?\\s+\\p{Lu}[\\p{Ll}]+");

    /** The document's opening third is where a demographics block conventionally sits, but for a
     * short document that proportional window can be tiny enough to cut off before a label even
     * finishes - "Referring provider: Jane Doe" is 29 characters, and a third of that is only 9.
     * A flat floor keeps short documents from being truncated away entirely while still capping
     * how far into a long document the generic {@link #LABELED_NAME_PAIR} pattern is allowed to
     * search, which is the false-positive guard the window exists for in the first place. */
    private static final int MIN_NAME_PAIR_WINDOW = 200;

    private static final List<Pattern> DEMOGRAPHIC_LABELS = List.of(
            Pattern.compile("(?i)\\bpatient\\s*:"),
            Pattern.compile("(?i)\\bname\\s*:"),
            Pattern.compile("(?i)\\bDOB\\s*:"),
            Pattern.compile("(?i)\\bMRN\\s*:"),
            Pattern.compile("(?i)\\baddress\\s*:"),
            Pattern.compile("(?i)\\bphone\\s*:"));

    /** Runs every heuristic and returns whether any fired, and which. Never throws. */
    public PhiScanResult scan(String rawText) {
        List<String> reasons = new ArrayList<>();
        if (rawText == null || rawText.isBlank()) {
            return new PhiScanResult(false, reasons);
        }

        if (matchesSsn(rawText)) reasons.add("SSN_LIKE");
        if (MRN.matcher(rawText).find()) reasons.add("MRN_LIKE");
        if (matchesLabeledDob(rawText)) reasons.add("DOB_LABELED");
        if (UNLABELED_BIRTH_PHRASE.matcher(rawText).find()) reasons.add("DOB_PROSE");
        if (PHONE.matcher(rawText).find()) reasons.add("PHONE_NUMBER");
        if (EMAIL.matcher(rawText).find()) reasons.add("EMAIL_ADDRESS");
        if (matchesAddress(rawText)) reasons.add("ADDRESS_LIKE");
        if (matchesNameNearHeader(rawText)) reasons.add("NAME_NEAR_HEADER");
        if (CLINICIAN_NAME.matcher(rawText).find()) reasons.add("CLINICIAN_NAME");
        if (matchesDemographicsBlock(rawText)) reasons.add("DEMOGRAPHICS_BLOCK");

        return new PhiScanResult(!reasons.isEmpty(), reasons);
    }

    /** The dashed form is unambiguous. The bare 9-digit form is too noisy on its own - an
     * accession or lot number is also nine digits - so it only counts near a label. */
    private boolean matchesSsn(String text) {
        if (SSN_DASHED.matcher(text).find()) {
            return true;
        }
        Matcher bare = SSN_BARE.matcher(text);
        while (bare.find()) {
            if (hasNearbyMatch(SSN_LABEL, text, bare.start(), bare.end(), 40)) {
                return true;
            }
        }
        return false;
    }

    /** Flags on a DOB-style label paired with a date-shaped token nearby - never on a bare date,
     * since an ordinary clinical date ("diagnosed 2023-04") is expected content. */
    private boolean matchesLabeledDob(String text) {
        Matcher label = DOB_LABEL.matcher(text);
        while (label.find()) {
            if (hasNearbyMatch(DATE_TOKEN, text, label.start(), label.end(), 30)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesAddress(String text) {
        return STREET_SUFFIX.matcher(text).find()
                || ZIP_STATE.matcher(text).find()
                || ADDRESS_LABEL.matcher(text).find();
    }

    /** The hardest heuristic to get right, so deliberately generous: a name-ish label followed
     * by capitalized tokens, or a labeled pair of capitalized tokens in the document's opening
     * third, where a demographics block conventionally sits. */
    private boolean matchesNameNearHeader(String text) {
        if (NAME_HEADER_LABEL.matcher(text).find()) {
            return true;
        }
        if (UNLABELED_PATIENT_NAME.matcher(text).find()) {
            return true;
        }
        if (UNPUNCTUATED_TITLE_NAME.matcher(text).find()) {
            return true;
        }
        if (APPOSITION_NAME.matcher(text).find()) {
            return true;
        }
        int window = Math.max(Math.min(text.length(), MIN_NAME_PAIR_WINDOW), text.length() / 3);
        Matcher pair = LABELED_NAME_PAIR.matcher(text.substring(0, window));
        return pair.find();
    }

    /** Two or more demographic labels co-occurring within a short window is close to
     * unambiguous, and can stand on its own regardless of what else fired. */
    private boolean matchesDemographicsBlock(String text) {
        String[] lines = text.split("\\R");
        for (int i = 0; i < lines.length; i++) {
            int window = Math.min(lines.length, i + 5);
            StringBuilder block = new StringBuilder();
            for (int j = i; j < window; j++) {
                block.append(lines[j]).append('\n');
            }
            long hits = DEMOGRAPHIC_LABELS.stream()
                    .filter(p -> p.matcher(block).find())
                    .count();
            if (hits >= 2) {
                return true;
            }
        }
        return false;
    }

    private boolean hasNearbyMatch(Pattern pattern, String text, int start, int end, int window) {
        int from = Math.max(0, start - window);
        int to = Math.min(text.length(), end + window);
        return pattern.matcher(text.substring(from, to)).find();
    }
}
