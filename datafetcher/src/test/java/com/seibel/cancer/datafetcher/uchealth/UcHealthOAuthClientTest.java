package com.seibel.cancer.datafetcher.uchealth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seibel.cancer.common.domain.UcHealthOAuthToken;
import com.seibel.cancer.database.db.service.UcHealthOAuthTokenDbService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the token-lifecycle logic that has no counterpart in the CT.gov pipeline:
 * deciding when a stored token needs refreshing, and rejecting callbacks whose state
 * we never issued. The HTTP calls themselves are not exercised here - those get manual
 * verification against Epic's sandbox.
 */
class UcHealthOAuthClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private UcHealthOAuthProperties properties;
    private PkceChallengeStore pkceChallengeStore;
    private UcHealthOAuthTokenDbService tokenDbService;
    private UcHealthOAuthClient client;

    /** The form the stubbed token endpoint was called with, for asserting on grant params. */
    private MultiValueMap<String, String> capturedForm;

    /**
     * A client whose token endpoint is stubbed out, so the OAuth logic can be tested
     * without a live HTTP call to Epic.
     */
    private UcHealthOAuthClient clientReturningTokenResponse(String json) {
        return new UcHealthOAuthClient(properties, pkceChallengeStore, tokenDbService) {
            @Override
            JsonNode postToken(MultiValueMap<String, String> form, String operation) {
                capturedForm = form;
                try {
                    return OBJECT_MAPPER.readTree(json);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        };
    }

    @BeforeEach
    void setUp() {
        properties = new UcHealthOAuthProperties();
        properties.setClientId("test-client-id");
        properties.setAuthorizeUrl("https://sandbox.example.org/oauth2/authorize");
        properties.setTokenUrl("https://sandbox.example.org/oauth2/token");
        properties.setFhirBaseUrl("https://sandbox.example.org/api/FHIR/R4");
        properties.setRedirectUri("http://localhost:8080/api/uchealth/callback");
        properties.setScope("patient/Observation.read offline_access");

        pkceChallengeStore = new PkceChallengeStore();
        tokenDbService = mock(UcHealthOAuthTokenDbService.class);
        client = new UcHealthOAuthClient(properties, pkceChallengeStore, tokenDbService);
    }

    @Test
    void ensureValidToken_returnsStoredToken_whenNotNearExpiry() {
        UcHealthOAuthToken valid = UcHealthOAuthToken.builder()
                .extid("token-extid")
                .accessToken("still-good")
                .refreshToken("refresh-me-later")
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build();
        when(tokenDbService.findCurrent()).thenReturn(valid);

        UcHealthOAuthToken result = client.ensureValidToken();

        assertThat(result.getAccessToken()).isEqualTo("still-good");
        // No refresh attempted - so no update, and no call to the token endpoint.
        verify(tokenDbService, never()).update(anyString(), any(), any(), any(), any(), any());
    }

    @Test
    void ensureValidToken_refreshesAndPersists_whenTokenIsExpired() {
        UcHealthOAuthToken expired = UcHealthOAuthToken.builder()
                .extid("token-extid")
                .accessToken("stale")
                .refreshToken("old-refresh-token")
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build();
        when(tokenDbService.findCurrent()).thenReturn(expired);

        UcHealthOAuthClient stubbed = clientReturningTokenResponse(
                "{\"access_token\":\"fresh\",\"refresh_token\":\"new-refresh-token\","
                        + "\"expires_in\":3600,\"patient\":\"patient-123\",\"scope\":\"patient/Observation.read\"}");
        when(tokenDbService.update(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(UcHealthOAuthToken.builder().extid("token-extid").accessToken("fresh").build());

        UcHealthOAuthToken result = stubbed.ensureValidToken();

        assertThat(result.getAccessToken()).isEqualTo("fresh");
        assertThat(capturedForm.getFirst("grant_type")).isEqualTo("refresh_token");
        assertThat(capturedForm.getFirst("refresh_token")).isEqualTo("old-refresh-token");
        // Public client - no secret is ever sent on the refresh call either.
        assertThat(capturedForm.getFirst("client_secret")).isNull();

        verify(tokenDbService).update(eq("token-extid"), eq("fresh"), eq("new-refresh-token"),
                any(), eq("patient-123"), eq("patient/Observation.read"));
    }

    @Test
    void ensureValidToken_refreshes_whenExpiresAtIsWithinTheSkewWindow() {
        UcHealthOAuthToken almostExpired = UcHealthOAuthToken.builder()
                .extid("token-extid")
                .accessToken("about-to-die")
                .refreshToken("old-refresh-token")
                .expiresAt(LocalDateTime.now().plusSeconds(10))
                .build();
        when(tokenDbService.findCurrent()).thenReturn(almostExpired);

        UcHealthOAuthClient stubbed = clientReturningTokenResponse(
                "{\"access_token\":\"fresh\",\"expires_in\":3600}");
        when(tokenDbService.update(anyString(), any(), any(), any(), any(), any()))
                .thenReturn(UcHealthOAuthToken.builder().accessToken("fresh").build());

        assertThat(stubbed.ensureValidToken().getAccessToken()).isEqualTo("fresh");
        assertThat(capturedForm.getFirst("grant_type")).isEqualTo("refresh_token");
    }

    @Test
    void refresh_keepsExistingRefreshToken_whenEpicReturnsNone() {
        UcHealthOAuthToken expired = UcHealthOAuthToken.builder()
                .extid("token-extid")
                .refreshToken("old-refresh-token")
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build();

        UcHealthOAuthClient stubbed = clientReturningTokenResponse(
                "{\"access_token\":\"fresh\",\"expires_in\":3600}");

        stubbed.refresh(expired);

        // A null refresh token leaves the stored one untouched (DbService.update ignores nulls).
        verify(tokenDbService).update(eq("token-extid"), eq("fresh"), isNull(), any(), isNull(), isNull());
    }

    @Test
    void ensureValidToken_throws_whenNoAuthorizationExistsYet() {
        when(tokenDbService.findCurrent()).thenReturn(null);

        assertThatThrownBy(() -> client.ensureValidToken())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No UCHealth authorization on file");
    }

    @Test
    void refresh_throws_whenStoredTokenHasNoRefreshToken() {
        UcHealthOAuthToken noRefresh = UcHealthOAuthToken.builder()
                .extid("token-extid")
                .accessToken("stale")
                .expiresAt(LocalDateTime.now().minusMinutes(5))
                .build();

        assertThatThrownBy(() -> client.refresh(noRefresh))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no refresh token");
    }

    @Test
    void exchangeAuthorizationCode_rejectsUnknownState() {
        assertThatThrownBy(() -> client.exchangeAuthorizationCode("some-code", "never-issued-state"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unknown or expired OAuth state");

        verify(tokenDbService, never()).create(any(), any(), any(), any(), any());
    }

    @Test
    void buildAuthorizationUrl_includesPkceAndStateParams() {
        String url = client.buildAuthorizationUrl();

        assertThat(url).startsWith("https://sandbox.example.org/oauth2/authorize?");
        assertThat(url).contains("response_type=code");
        assertThat(url).contains("client_id=test-client-id");
        assertThat(url).contains("code_challenge_method=S256");
        assertThat(url).contains("code_challenge=");
        assertThat(url).contains("state=");
        // No client secret is ever sent - this is the public-client / PKCE flow.
        assertThat(url).doesNotContain("client_secret");
    }

    @Test
    void buildAuthorizationUrl_isAValidUri() {
        String url = client.buildAuthorizationUrl();

        // Regression: scope is space-delimited and redirect_uri contains "://" - without
        // encode() this threw "Illegal character in query" when the controller built the URI.
        assertThatCode(() -> URI.create(url)).doesNotThrowAnyException();
        // The space between scopes is what actually broke it - it must come out encoded.
        assertThat(url).doesNotContain(" ");
        assertThat(url).contains("scope=patient/Observation.read%20offline_access");
    }

    @Test
    void buildAuthorizationUrl_throws_whenNotConfigured() {
        UcHealthOAuthClient unconfigured = new UcHealthOAuthClient(
                new UcHealthOAuthProperties(), pkceChallengeStore, tokenDbService);

        assertThatThrownBy(unconfigured::buildAuthorizationUrl)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not configured");
    }
}
