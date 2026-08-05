package com.seibel.cancer.datafetcher.uchealth;

import com.fasterxml.jackson.databind.JsonNode;
import com.seibel.cancer.common.domain.UcHealthOAuthToken;
import com.seibel.cancer.database.db.service.UcHealthOAuthTokenDbService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;

/**
 * SMART on FHIR (OAuth 2.0) client for Epic, using the public-client / PKCE flow -
 * no client secret. Handles the authorization-code exchange, the refresh-token flow,
 * and persistence of the resulting token via UcHealthOAuthTokenDbService.
 *
 * Unlike ClinicalTrialsGovClient (an unauthenticated GET), this is patient-authorized
 * access: the patient logs into My Health Connection and consents before any token
 * exists. See .claude/UCHEALTH_INGESTION_PLAN.md for the full access model.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UcHealthOAuthClient {

    /** Refresh this far ahead of actual expiry, so a token can't die mid-ingestion. */
    private static final long EXPIRY_SKEW_SECONDS = 60;

    private final UcHealthOAuthProperties properties;
    private final PkceChallengeStore pkceChallengeStore;
    private final UcHealthOAuthTokenDbService tokenDbService;

    private final RestClient restClient = RestClient.builder().build();

    /**
     * Builds the Epic authorization URL the patient's browser gets redirected to.
     * Registers a fresh state + PKCE verifier as a side effect - the matching
     * {@link #exchangeAuthorizationCode} call consumes them.
     */
    public String buildAuthorizationUrl() {
        requireConfigured();

        PkceChallengeStore.Pending pending = pkceChallengeStore.start();

        String url = UriComponentsBuilder.fromUriString(properties.getAuthorizeUrl())
                .queryParam("response_type", "code")
                .queryParam("client_id", properties.getClientId())
                .queryParam("redirect_uri", properties.getRedirectUri())
                .queryParam("scope", properties.getScope())
                .queryParam("state", pending.state())
                .queryParam("aud", properties.getFhirBaseUrl())
                .queryParam("code_challenge", pkceChallengeStore.codeChallenge(pending.codeVerifier()))
                .queryParam("code_challenge_method", "S256")
                // encode() is required: scope is space-delimited and redirect_uri/aud contain
                // "://", none of which are legal raw in a query string.
                .encode()
                .build()
                .toUriString();

        log.info("buildAuthorizationUrl(): state={}", pending.state());
        return url;
    }

    /**
     * Exchanges an authorization code for an access + refresh token and stores it.
     * The state must match a pending authorization started by
     * {@link #buildAuthorizationUrl} - an unrecognized state is rejected rather than
     * exchanged, since it means the callback wasn't one we initiated.
     */
    public UcHealthOAuthToken exchangeAuthorizationCode(String code, String state) {
        requireConfigured();

        String codeVerifier = pkceChallengeStore.consumeVerifier(state);
        if (codeVerifier == null) {
            throw new IllegalStateException(
                    "Unknown or expired OAuth state - start the flow again at /api/uchealth/authorize");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", properties.getRedirectUri());
        form.add("client_id", properties.getClientId());
        form.add("code_verifier", codeVerifier);

        JsonNode response = postToken(form, "exchangeAuthorizationCode");

        String accessToken = requireField(response, "access_token");
        String refreshToken = response.path("refresh_token").asText(null);
        LocalDateTime expiresAt = toExpiresAt(response.path("expires_in").asLong(0));
        String patientFhirId = response.path("patient").asText(null);
        String scope = response.path("scope").asText(null);

        UcHealthOAuthToken stored = tokenDbService.create(
                accessToken, refreshToken, expiresAt, patientFhirId, scope);

        log.info("exchangeAuthorizationCode(): stored token, patientFhirId={}, expiresAt={}",
                patientFhirId, expiresAt);
        return stored;
    }

    /**
     * Returns a token guaranteed valid for the next {@value #EXPIRY_SKEW_SECONDS}
     * seconds, refreshing it first if needed. Throws if no authorization exists yet -
     * the patient has to complete the browser flow at least once.
     */
    public UcHealthOAuthToken ensureValidToken() {
        UcHealthOAuthToken current = tokenDbService.findCurrent();
        if (current == null) {
            throw new IllegalStateException(
                    "No UCHealth authorization on file - complete /api/uchealth/authorize first");
        }
        return isExpiring(current) ? refresh(current) : current;
    }

    /**
     * Trades the stored refresh token for a fresh access token, updating the same row.
     * Epic may or may not return a new refresh token; when it doesn't, the existing one
     * stays in place (update() ignores nulls).
     */
    public UcHealthOAuthToken refresh(UcHealthOAuthToken token) {
        requireConfigured();

        if (token.getRefreshToken() == null || token.getRefreshToken().isBlank()) {
            throw new IllegalStateException(
                    "Stored token has no refresh token - re-authorize at /api/uchealth/authorize");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", token.getRefreshToken());
        form.add("client_id", properties.getClientId());

        JsonNode response = postToken(form, "refresh");

        String accessToken = requireField(response, "access_token");
        String refreshToken = response.path("refresh_token").asText(null);
        LocalDateTime expiresAt = toExpiresAt(response.path("expires_in").asLong(0));
        String patientFhirId = response.path("patient").asText(null);
        String scope = response.path("scope").asText(null);

        UcHealthOAuthToken updated = tokenDbService.update(
                token.getExtid(), accessToken, refreshToken, expiresAt, patientFhirId, scope);

        log.info("refresh(): refreshed token, expiresAt={}", expiresAt);
        return updated;
    }

    private boolean isExpiring(UcHealthOAuthToken token) {
        LocalDateTime expiresAt = token.getExpiresAt();
        return expiresAt == null || expiresAt.isBefore(LocalDateTime.now().plusSeconds(EXPIRY_SKEW_SECONDS));
    }

    // Package-private so tests can stub the token endpoint without a live network call.
    JsonNode postToken(MultiValueMap<String, String> form, String operation) {
        try {
            JsonNode response = restClient.post()
                    .uri(properties.getTokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                throw new IllegalStateException("Empty response from token endpoint");
            }
            return response;

        } catch (Exception e) {
            // Never log the form body - it carries the code / refresh token.
            log.error("{}(): token endpoint call failed", operation, e);
            throw new IllegalStateException(operation + " failed: " + e.getMessage(), e);
        }
    }

    private LocalDateTime toExpiresAt(long expiresInSeconds) {
        // Epic issues ~1 hour tokens; fall back to that if expires_in is absent.
        long seconds = expiresInSeconds > 0 ? expiresInSeconds : 3600;
        return LocalDateTime.now().plusSeconds(seconds);
    }

    private String requireField(JsonNode response, String field) {
        String value = response.path(field).asText(null);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Token response missing '" + field + "'");
        }
        return value;
    }

    private void requireConfigured() {
        if (!properties.isConfigured()) {
            throw new IllegalStateException(
                    "UCHealth OAuth is not configured - set uchealth.client-id / authorize-url / "
                            + "token-url / redirect-uri (see .env)");
        }
    }
}
