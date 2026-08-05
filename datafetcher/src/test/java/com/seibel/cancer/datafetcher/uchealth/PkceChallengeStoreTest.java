package com.seibel.cancer.datafetcher.uchealth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PkceChallengeStoreTest {

    private final PkceChallengeStore store = new PkceChallengeStore();

    @Test
    void start_issuesDistinctStateAndVerifierEachTime() {
        PkceChallengeStore.Pending first = store.start();
        PkceChallengeStore.Pending second = store.start();

        assertThat(first.state()).isNotBlank().isNotEqualTo(second.state());
        assertThat(first.codeVerifier()).isNotBlank().isNotEqualTo(second.codeVerifier());
    }

    @Test
    void consumeVerifier_returnsVerifierOnce_thenTreatsStateAsUnknown() {
        PkceChallengeStore.Pending pending = store.start();

        assertThat(store.consumeVerifier(pending.state())).isEqualTo(pending.codeVerifier());
        // Replaying the same callback must not exchange a second time.
        assertThat(store.consumeVerifier(pending.state())).isNull();
    }

    @Test
    void consumeVerifier_returnsNull_forUnknownOrMissingState() {
        assertThat(store.consumeVerifier("never-issued")).isNull();
        assertThat(store.consumeVerifier(null)).isNull();
    }

    @Test
    void codeChallenge_isDeterministicUrlSafeS256() {
        // RFC 7636 appendix B test vector.
        String verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";

        assertThat(store.codeChallenge(verifier))
                .isEqualTo("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM");
    }
}
