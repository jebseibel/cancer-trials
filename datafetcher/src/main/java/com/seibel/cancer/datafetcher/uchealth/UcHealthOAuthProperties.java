package com.seibel.cancer.datafetcher.uchealth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Epic / SMART on FHIR connection settings. Point these at Epic's sandbox first;
 * only switch to UCHealth's production endpoints after the sandbox round-trip works.
 * Values come from the environment (.env) - nothing here is committed.
 */
@Data
@Component
@ConfigurationProperties(prefix = "uchealth")
public class UcHealthOAuthProperties {

    /** Epic-issued client id for the registered patient-facing app. */
    private String clientId;

    /** OAuth authorization endpoint (Epic sandbox or UCHealth production). */
    private String authorizeUrl;

    /** OAuth token endpoint, used for both code exchange and refresh. */
    private String tokenUrl;

    /** FHIR R4 base URL, used by the FHIR client (not by the OAuth flow itself). */
    private String fhirBaseUrl;

    /** Must exactly match the redirect URI registered with Epic. */
    private String redirectUri;

    /** SMART on FHIR scopes requested at authorization time. */
    private String scope;

    public boolean isConfigured() {
        return hasText(clientId) && hasText(authorizeUrl) && hasText(tokenUrl) && hasText(redirectUri);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
