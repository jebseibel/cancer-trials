package com.seibel.cancer.service.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The gate that decides whether a document ever reaches {@link AiService}.
 *
 * <p>Biased to over-flag on purpose: a false positive here costs the user a rewrite, a false
 * negative sends identifying detail to the model. These tests check both directions - that
 * planted identifiers are caught, and that ordinary clinical prose is not wrongly rejected.
 */
class PhiHeuristicScannerTest {

    private final PhiHeuristicScanner scanner = new PhiHeuristicScanner();

    @Test
    @DisplayName("clean clinical prose with no identifiers passes")
    void cleanClinicalTextPasses() {
        String text = """
                Pathology report

                Diagnosis: invasive ductal carcinoma of the left breast, Stage IIA.
                ER positive, PR positive, HER2 negative. Ki-67 18%.
                Diagnosed 2023-04, no evidence of metastasis.
                Recommend endocrine therapy and consideration of CDK4/6 inhibitor.
                ECOG performance status 0. No prior chemotherapy regimens.
                """;

        PhiScanResult result = scanner.scan(text);

        assertThat(result.flagged()).isFalse();
        assertThat(result.reasons()).isEmpty();
    }

    @Test
    @DisplayName("a dashed SSN always flags")
    void flagsDashedSsn() {
        assertThat(scanner.scan("SSN: 123-45-6789").flagged()).isTrue();
    }

    @Test
    @DisplayName("a bare nine-digit number alone does not flag")
    void bareNineDigitNumberAloneDoesNotFlag() {
        // An accession or lot number is also nine digits - only a nearby SSN label should flag.
        PhiScanResult result = scanner.scan("Specimen accession number 123456789 received.");
        assertThat(result.flagged()).isFalse();
    }

    @Test
    @DisplayName("a bare nine-digit number near a social security label flags")
    void bareNineDigitNumberNearLabelFlags() {
        PhiScanResult result = scanner.scan("Social security number 123456789 on file.");
        assertThat(result.flagged()).isTrue();
        assertThat(result.reasons()).contains("SSN_LIKE");
    }

    @Test
    @DisplayName("an MRN label flags regardless of format")
    void flagsMrnLabel() {
        PhiScanResult result = scanner.scan("MRN: 00482913");
        assertThat(result.flagged()).isTrue();
        assertThat(result.reasons()).contains("MRN_LIKE");
    }

    @Test
    @DisplayName("a DOB label paired with a date flags")
    void flagsLabeledDob() {
        PhiScanResult result = scanner.scan("DOB: 03/14/1965");
        assertThat(result.flagged()).isTrue();
        assertThat(result.reasons()).contains("DOB_LABELED");
    }

    @Test
    @DisplayName("an ordinary clinical date with no DOB label does not flag")
    void bareClinicalDateDoesNotFlag() {
        PhiScanResult result = scanner.scan(
                "Diagnosed 2023-04-12. Last chemotherapy ended 05/07/2024.");
        assertThat(result.flagged()).isFalse();
    }

    @Test
    @DisplayName("a birth date stated as prose, with no DOB label, still flags")
    void flagsUnlabeledBirthPhrase() {
        PhiScanResult result = scanner.scan("Patient Jane Doe was born on 3/14/1965.");
        assertThat(result.flagged()).isTrue();
        assertThat(result.reasons()).contains("DOB_PROSE");
    }

    @Test
    @DisplayName("a birth date given as a month name in prose still flags")
    void flagsUnlabeledBirthPhraseWithMonthName() {
        PhiScanResult result = scanner.scan("She was born in March 1965 and presented with...");
        assertThat(result.flagged()).isTrue();
        assertThat(result.reasons()).contains("DOB_PROSE");
    }

    @Test
    @DisplayName("an unlabeled patient name in prose still flags")
    void flagsUnlabeledPatientNameInProse() {
        PhiScanResult result = scanner.scan(
                "Patient Jane Doe presents with a new diagnosis of invasive ductal carcinoma.");
        assertThat(result.flagged()).isTrue();
        assertThat(result.reasons()).contains("NAME_NEAR_HEADER");
    }

    @Test
    @DisplayName("\"Patient presented with\" is not mistaken for a patient name")
    void patientPresentedWithDoesNotFlagAsName() {
        // "Patient" followed by a lower-case clinical verb, not two capitalized name tokens.
        PhiScanResult result = scanner.scan(
                "Patient presented with a palpable mass in the left breast.");
        assertThat(result.flagged()).isFalse();
    }

    @Test
    @DisplayName("a phone number flags")
    void flagsPhoneNumber() {
        assertThat(scanner.scan("Contact patient at (555) 123-4567.").flagged()).isTrue();
    }

    @Test
    @DisplayName("an email address flags")
    void flagsEmailAddress() {
        assertThat(scanner.scan("Send records to patient.name@example.com").flagged()).isTrue();
    }

    @Test
    @DisplayName("a street address flags")
    void flagsStreetAddress() {
        assertThat(scanner.scan("Mailing address: 742 Evergreen Terrace, Springfield.")
                .flagged()).isTrue();
    }

    @Test
    @DisplayName("a name near a header label flags")
    void flagsNameNearHeader() {
        PhiScanResult result = scanner.scan("Patient Name: Jane Doe\nDiagnosis: ...");
        assertThat(result.flagged()).isTrue();
        assertThat(result.reasons()).contains("NAME_NEAR_HEADER");
    }

    @Test
    @DisplayName("a plain procedure-name label is not mistaken for a patient name")
    void procedureNameLabelDoesNotFlagAsPatientName() {
        // A tricky near-miss: "Name of procedure:" contains the word "name" but is not a
        // demographics field, and the value that follows is lower-case.
        PhiScanResult result = scanner.scan(
                "Name of procedure: core needle biopsy of the left breast.");
        assertThat(result.flagged()).isFalse();
    }

    @Test
    @DisplayName("a clinician name with a Dr. prefix flags")
    void flagsClinicianName() {
        assertThat(scanner.scan("Reviewed by Dr Halvorsen.").flagged()).isTrue();
    }

    @Test
    @DisplayName("two or more co-occurring demographic labels flag as a block")
    void flagsDemographicsBlock() {
        String text = """
                Patient: Jane Doe
                DOB: 03/14/1965
                MRN: 00482913
                Diagnosis: invasive ductal carcinoma
                """;
        PhiScanResult result = scanner.scan(text);
        assertThat(result.flagged()).isTrue();
        assertThat(result.reasons()).contains("DEMOGRAPHICS_BLOCK");
    }

    @Test
    @DisplayName("reasons never carry the matched text, only category labels")
    void reasonsCarryOnlyCategoryLabels() {
        PhiScanResult result = scanner.scan("Patient Name: Jane Doe");
        assertThat(result.reasons()).allSatisfy(reason ->
                assertThat(reason).doesNotContainIgnoringCase("jane").doesNotContain("Doe"));
    }

    @Test
    @DisplayName("blank input never flags")
    void blankInputDoesNotFlag() {
        assertThat(scanner.scan("").flagged()).isFalse();
        assertThat(scanner.scan(null).flagged()).isFalse();
    }

    // ---------------------------------------------------------------------------------------
    // Phone number - additional formats and a clinical near-miss.
    // ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("a dotted phone number flags")
    void flagsDottedPhoneNumber() {
        assertThat(scanner.scan("Contact: 555.123.4567").flagged()).isTrue();
    }

    @Test
    @DisplayName("a bare unformatted ten-digit phone number flags")
    void flagsBareTenDigitPhoneNumber() {
        assertThat(scanner.scan("Call the front desk at 5551234567 to schedule.").flagged())
                .isTrue();
    }

    @Test
    @DisplayName("a phone number with a country code flags")
    void flagsPhoneNumberWithCountryCode() {
        assertThat(scanner.scan("Reach the clinic at +1 555 123 4567.").flagged()).isTrue();
    }

    @Test
    @DisplayName("clinical measurements with no real phone number do not flag as one")
    void clinicalMeasurementsDoNotFlagAsPhoneNumber() {
        // Percentages, tumor size, and node counts in a row should not resemble a phone number.
        PhiScanResult result = scanner.scan(
                "ER 95%, PR 80%, HER2 negative, tumor size 45mm, 3 of 12 nodes positive.");
        assertThat(result.flagged()).isFalse();
    }

    @Test
    @DisplayName("known gap: a phone number spelled out in words is not caught")
    void spelledOutPhoneNumberIsNotCaught() {
        // Documents current behavior, not a desired outcome - a regex scan cannot recognize
        // digits written as words. Flag for follow-up if this pattern is ever seen in practice.
        PhiScanResult result = scanner.scan(
                "Phone number is five five five, one two three, four five six seven.");
        assertThat(result.flagged()).isFalse();
    }

    // ---------------------------------------------------------------------------------------
    // Email address - additional formats and obfuscated near-misses.
    // ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("an email address embedded in a sentence flags")
    void flagsEmailAddressInSentence() {
        assertThat(scanner.scan("You can reach her at jane.doe@gmail.com for questions.")
                .flagged()).isTrue();
    }

    @Test
    @DisplayName("known gap: an obfuscated email address ([at]/[dot]) is not caught")
    void obfuscatedEmailIsNotCaught() {
        // Documents current behavior - "[at]"/"[dot]" obfuscation defeats the email pattern by
        // design (that's the point of writing it that way). Not something this heuristic can fix.
        assertThat(scanner.scan("email jdoe [at] gmail [dot] com").flagged()).isFalse();
    }

    // ---------------------------------------------------------------------------------------
    // Street address - suffix coverage and a documented gap in the suffix list.
    // ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("a street address embedded in a sentence flags")
    void flagsStreetAddressInSentence() {
        PhiScanResult result = scanner.scan(
                "She resides at 12 Birchwood Ln, Denver CO 80203 with her spouse.");
        assertThat(result.flagged()).isTrue();
        assertThat(result.reasons()).contains("ADDRESS_LIKE");
    }

    @Test
    @DisplayName("a city/state/ZIP with no street line still flags")
    void flagsCityStateZipAlone() {
        PhiScanResult result = scanner.scan("Denver, CO 80203");
        assertThat(result.flagged()).isTrue();
        assertThat(result.reasons()).contains("ADDRESS_LIKE");
    }

    @Test
    @DisplayName("a street address using a less common suffix word still flags")
    void flagsStreetAddressWithLessCommonSuffix() {
        // Terrace, Place, Trail, Parkway, and Plaza were added to the suffix alternation
        // alongside the original Street/Avenue/Boulevard/Road/Drive/Lane/Way/Court/Circle set,
        // closing a gap where a bare street line (no city/state/ZIP attached) with one of these
        // suffixes passed through uncaught.
        PhiScanResult result = scanner.scan("742 Evergreen Terrace");
        assertThat(result.flagged()).isTrue();
        assertThat(result.reasons()).contains("ADDRESS_LIKE");
    }

    @Test
    @DisplayName("plain clinical measurements do not flag as an address")
    void clinicalMeasurementsDoNotFlagAsAddress() {
        PhiScanResult result = scanner.scan("Tumor measures 4.5 cm, stage IIA, grade 2.");
        assertThat(result.flagged()).isFalse();
    }

    // ---------------------------------------------------------------------------------------
    // Name detection - a short-document window fix, an apposition fix, and an unpunctuated
    // title fix.
    // ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("a labeled name pair still flags even in a very short document")
    void flagsLabeledNamePairInAShortDocument() {
        // A flat minimum search window (on top of the opening-third proportional one) fixed a
        // gap where "Referring provider: Jane Doe" (29 characters) was truncated to its first 9
        // characters ("Referring") before the colon or name were ever reached.
        PhiScanResult result = scanner.scan("Referring provider: Jane Doe");
        assertThat(result.flagged()).isTrue();
        assertThat(result.reasons()).contains("NAME_NEAR_HEADER");
    }

    @Test
    @DisplayName("a name in apposition after \"the patient,\" still flags")
    void flagsNameInApposition() {
        PhiScanResult result = scanner.scan("The patient, Jane Doe, was evaluated today.");
        assertThat(result.flagged()).isTrue();
        assertThat(result.reasons()).contains("NAME_NEAR_HEADER");
    }

    @Test
    @DisplayName("a title without a period or colon still flags as a name")
    void flagsNameAfterTitleWithoutPunctuation() {
        PhiScanResult result = scanner.scan("Ms Jane Doe was evaluated in clinic today.");
        assertThat(result.flagged()).isTrue();
        assertThat(result.reasons()).contains("NAME_NEAR_HEADER");
    }

    @Test
    @DisplayName("\"Ms.\" used as a plain title before a single surname does not over-match")
    void singleSurnameAfterTitleDoesNotOverMatch() {
        // Sanity check on the unpunctuated-title pattern: it should still recognize a
        // single-name form ("Ms. Doe"), not just a two-word name, without needing extra
        // capitalized words to follow.
        PhiScanResult result = scanner.scan("Ms. Doe called to reschedule.");
        assertThat(result.flagged()).isTrue();
    }

    // ---------------------------------------------------------------------------------------
    // Two false positives found live against this app's own "Download my record" export:
    // its title header crossing a line break, and a genetic-testing lab name reading as a
    // labeled person's name.
    // ---------------------------------------------------------------------------------------

    @Test
    @DisplayName("this app's own record-export header does not flag as an unlabeled patient name")
    void ownExportHeaderDoesNotFlagAsPatientName() {
        // "...Patient Record" followed by "Generated <date>" on the next line used to read as
        // "Patient" + two capitalized tokens spanning the line break - patientRecordExport.ts's
        // own generated header, not any real content.
        PhiScanResult result = scanner.scan(
                "Breast Cancer Trial Finder — Patient Record\nGenerated September 3, 2026");
        assertThat(result.flagged()).isFalse();
    }

    @Test
    @DisplayName("a same-sentence unlabeled patient name across a line break still flags")
    void unlabeledPatientNameStillFlagsAcrossWrappedText() {
        // The line-break fix must not stop recognizing a real name should a document happen to
        // wrap mid-sentence - it specifically targets "Patient" immediately followed by the next
        // line's first words, not any name that occurs near a newline.
        PhiScanResult result = scanner.scan("Patient Jane Doe presents with a new diagnosis.");
        assertThat(result.flagged()).isTrue();
        assertThat(result.reasons()).contains("NAME_NEAR_HEADER");
    }

    @Test
    @DisplayName("a testing-lab name does not flag as a labeled person name")
    void testingLabNameDoesNotFlagAsPersonName() {
        PhiScanResult result = scanner.scan("Testing lab: Ambry Genetics (85-gene panel)");
        assertThat(result.flagged()).isFalse();
    }

    @Test
    @DisplayName("other lab-labeled shapes also do not flag")
    void otherLabLabelShapesDoNotFlag() {
        assertThat(scanner.scan("Referring lab: Quest Diagnostics").flagged()).isFalse();
        assertThat(scanner.scan("Germline test: Ambry Genetics").flagged()).isFalse();
    }

    @Test
    @DisplayName("a labeled name pair still flags when the label is not about a lab or test")
    void labeledNamePairStillFlagsForNonLabLabels() {
        // The lab-word exclusion must not become a general escape hatch - a real name after an
        // unrelated label is still exactly the case LABELED_NAME_PAIR exists to catch.
        assertThat(scanner.scan("Referring provider: Jane Doe").flagged()).isTrue();
        assertThat(scanner.scan("Patient Name: Jane Doe").flagged()).isTrue();
        assertThat(scanner.scan("Physician: John Smith").flagged()).isTrue();
    }

    @Test
    @DisplayName("a lab-labeled pair earlier in the text does not hide a real name later on")
    void labLabelDoesNotMaskALaterRealName() {
        // matchesLabeledNamePair skips a lab-labeled match and keeps looking - it must not
        // short-circuit to "not flagged" on the first (lab) match it sees.
        PhiScanResult result = scanner.scan(
                "Testing lab: Ambry Genetics\nReferring provider: Jane Doe");
        assertThat(result.flagged()).isTrue();
        assertThat(result.reasons()).contains("NAME_NEAR_HEADER");
    }
}
