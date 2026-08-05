package com.seibel.cancer.datafetcher.uchealth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the PKCE code_verifier for an in-flight authorization, keyed by the OAuth
 * state parameter, between the /authorize redirect and the /callback return.
 *
 * In-memory on purpose: a pending authorization only has to survive the few seconds
 * the patient spends on UCHealth's login page, and this app authorizes one patient by
 * hand. A backend restart mid-login just means starting the flow over.
 */
@Slf4j
@Component
public class PkceChallengeStore {

    /** Pending authorizations older than this are discarded - the login was abandoned. */
    private static final Duration PENDING_TTL = Duration.ofMinutes(15);

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final Map<String, Pending> pendingByState = new ConcurrentHashMap<>();

    /**
     * Creates a new state + code_verifier pair, stores it, and returns it so the caller
     * can build the authorization URL.
     */
    public Pending start() {
        purgeExpired();

        String state = randomUrlSafe(32);
        String codeVerifier = randomUrlSafe(64);
        Pending pending = new Pending(state, codeVerifier, LocalDateTime.now());

        pendingByState.put(state, pending);
        log.info("start(): new pending authorization, state={}", state);
        return pending;
    }

    /**
     * Consumes the pending authorization for the given state, returning its
     * code_verifier. Returns null if the state is unknown or expired - which means the
     * callback is unsolicited or too late, and the token exchange must not proceed.
     */
    public String consumeVerifier(String state) {
        purgeExpired();

        if (state == null) {
            return null;
        }
        Pending pending = pendingByState.remove(state);
        if (pending == null) {
            log.warn("consumeVerifier(): no pending authorization for state={}", state);
            return null;
        }
        return pending.codeVerifier();
    }

    /** S256 challenge derived from the verifier, as sent on the authorization request. */
    public String codeChallenge(String codeVerifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return URL_ENCODER.encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive PKCE code challenge", e);
        }
    }

    private void purgeExpired() {
        LocalDateTime cutoff = LocalDateTime.now().minus(PENDING_TTL);
        pendingByState.values().removeIf(pending -> pending.createdAt().isBefore(cutoff));
    }

    private String randomUrlSafe(int byteCount) {
        byte[] bytes = new byte[byteCount];
        RANDOM.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }

    public record Pending(String state, String codeVerifier, LocalDateTime createdAt) {
    }
}
