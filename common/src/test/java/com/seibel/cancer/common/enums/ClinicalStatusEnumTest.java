package com.seibel.cancer.common.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the clinical distinctions these vocabularies exist to preserve.
 *
 * <p>The parsing tests matter because every one of these is stored as a plain varchar, so a
 * value that does not map must degrade to UNKNOWN rather than throw. The semantic tests matter
 * more: each asserts a distinction that a boolean would collapse, and collapsing one silently
 * mis-ranks trials rather than producing an error anyone would notice.
 */
class ClinicalStatusEnumTest {

    @Nested
    @DisplayName("VariantStatus")
    class Variant {

        @Test
        @DisplayName("not-tested and not-detected are different answers")
        void untestedIsNotTheSameAsNegative() {
            // The whole reason the vocabulary has five states. A trial requiring BRCA1 is a
            // genuine mismatch for a tested-negative patient and an open question for an
            // untested one - and this app was built against a real record with a negative
            // germline panel, which is the former case.
            assertThat(VariantStatus.NOT_DETECTED.isRuledOut()).isTrue();
            assertThat(VariantStatus.NOT_TESTED.isRuledOut()).isFalse();
            assertThat(VariantStatus.NOT_TESTED.isUnresolved()).isTrue();
        }

        @Test
        @DisplayName("a VUS is a question, never a fit")
        void vusIsNeverDetected() {
            assertThat(VariantStatus.VUS.isDetected()).isFalse();
            assertThat(VariantStatus.VUS.isRuledOut()).isFalse();
            assertThat(VariantStatus.VUS.isUnresolved()).isTrue();
        }

        @Test
        @DisplayName("only DETECTED counts as present")
        void onlyDetectedIsDetected() {
            assertThat(VariantStatus.DETECTED.isDetected()).isTrue();
            for (VariantStatus s : VariantStatus.values()) {
                if (s != VariantStatus.DETECTED) {
                    assertThat(s.isDetected()).as("%s must not read as detected", s).isFalse();
                }
            }
        }

        @Test
        @DisplayName("unmapped stored values degrade to UNKNOWN rather than throwing")
        void parsingIsForgiving() {
            assertThat(VariantStatus.fromValue("detected")).isEqualTo(VariantStatus.DETECTED);
            assertThat(VariantStatus.fromValue("  NOT_DETECTED  ")).isEqualTo(VariantStatus.NOT_DETECTED);
            assertThat(VariantStatus.fromValue(null)).isEqualTo(VariantStatus.UNKNOWN);
            assertThat(VariantStatus.fromValue("")).isEqualTo(VariantStatus.UNKNOWN);
            assertThat(VariantStatus.fromValue("probably?")).isEqualTo(VariantStatus.UNKNOWN);
        }
    }

    @Nested
    @DisplayName("TreatmentStatus")
    class Treatment {

        @Test
        @DisplayName("on a drug now is not the same as having progressed on it")
        void currentIsNotProgressed() {
            // The concrete failure this vocabulary prevents: a patient on a CDK4/6 inhibitor who
            // has not progressed. A boolean priorCdk46=true would route them to post-CDK4/6
            // trials - the wrong half of the corpus.
            assertThat(TreatmentStatus.CURRENT.hasProgressedOn()).isFalse();
            assertThat(TreatmentStatus.CURRENT.isNaive()).isFalse();
            assertThat(TreatmentStatus.PROGRESSED.hasProgressedOn()).isTrue();
        }

        @Test
        @DisplayName("stopping for toxicity is not progression")
        void stoppedForOtherReasonsIsNotProgression() {
            assertThat(TreatmentStatus.STOPPED_OTHER.hasProgressedOn()).isFalse();
            assertThat(TreatmentStatus.STOPPED_OTHER.hasReceived()).isTrue();
            assertThat(TreatmentStatus.STOPPED_OTHER.isNaive()).isFalse();
        }

        @Test
        @DisplayName("only NEVER is treatment-naive")
        void onlyNeverIsNaive() {
            assertThat(TreatmentStatus.NEVER.isNaive()).isTrue();
            assertThat(TreatmentStatus.NEVER.hasReceived()).isFalse();
            for (TreatmentStatus s : TreatmentStatus.values()) {
                if (s != TreatmentStatus.NEVER) {
                    assertThat(s.isNaive()).as("%s must not read as naive", s).isFalse();
                }
            }
        }

        @Test
        @DisplayName("UNKNOWN claims neither exposure nor naivety")
        void unknownClaimsNothing() {
            assertThat(TreatmentStatus.UNKNOWN.isNaive()).isFalse();
            assertThat(TreatmentStatus.UNKNOWN.hasReceived()).isFalse();
            assertThat(TreatmentStatus.UNKNOWN.hasProgressedOn()).isFalse();
            assertThat(TreatmentStatus.UNKNOWN.isUnresolved()).isTrue();
        }

        @Test
        @DisplayName("unmapped stored values degrade to UNKNOWN")
        void parsingIsForgiving() {
            assertThat(TreatmentStatus.fromValue("current")).isEqualTo(TreatmentStatus.CURRENT);
            assertThat(TreatmentStatus.fromValue(" PROGRESSED ")).isEqualTo(TreatmentStatus.PROGRESSED);
            assertThat(TreatmentStatus.fromValue(null)).isEqualTo(TreatmentStatus.UNKNOWN);
            assertThat(TreatmentStatus.fromValue("stopped")).isEqualTo(TreatmentStatus.UNKNOWN);
        }
    }

    @Nested
    @DisplayName("ReceptorStatus")
    class Receptor {

        @Test
        @DisplayName("positive and negative are never conflated")
        void polarityIsDistinct() {
            // The axis embeddings cannot see: one token separates "HR-positive HER2-negative"
            // from "HR-negative HER2-negative", and the two are opposite eligibility answers.
            assertThat(ReceptorStatus.POSITIVE.isPositive()).isTrue();
            assertThat(ReceptorStatus.POSITIVE.isNegative()).isFalse();
            assertThat(ReceptorStatus.NEGATIVE.isNegative()).isTrue();
            assertThat(ReceptorStatus.NEGATIVE.isPositive()).isFalse();
        }

        @Test
        @DisplayName("UNKNOWN asserts neither polarity, so it can render amber")
        void unknownAssertsNeitherPolarity() {
            assertThat(ReceptorStatus.UNKNOWN.isPositive()).isFalse();
            assertThat(ReceptorStatus.UNKNOWN.isNegative()).isFalse();
            assertThat(ReceptorStatus.UNKNOWN.isUnresolved()).isTrue();
        }

        @Test
        @DisplayName("accepts the shorthand that appears in real reports")
        void parsesShorthand() {
            assertThat(ReceptorStatus.fromValue("+")).isEqualTo(ReceptorStatus.POSITIVE);
            assertThat(ReceptorStatus.fromValue("pos")).isEqualTo(ReceptorStatus.POSITIVE);
            assertThat(ReceptorStatus.fromValue("-")).isEqualTo(ReceptorStatus.NEGATIVE);
            assertThat(ReceptorStatus.fromValue("NEG")).isEqualTo(ReceptorStatus.NEGATIVE);
            assertThat(ReceptorStatus.fromValue("equivocal")).isEqualTo(ReceptorStatus.UNKNOWN);
            assertThat(ReceptorStatus.fromValue(null)).isEqualTo(ReceptorStatus.UNKNOWN);
        }
    }
}
