package com.seibel.cancer.web.controller;

import com.seibel.cancer.common.domain.UcHealthOAuthToken;
import com.seibel.cancer.datafetcher.uchealth.UcHealthOAuthClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * The browser-driven half of UCHealth ingestion: the patient logs into My Health
 * Connection and consents, and Epic redirects back here with an authorization code.
 * Everything after this (fetch, stage, normalize) is server-side and uses the stored
 * token. Unlike the CT.gov flow, this cannot be driven from curl or Swagger alone -
 * it needs a real browser round-trip.
 */
@Slf4j
@RestController
@RequestMapping("/api/uchealth")
@Validated
@Tag(name = "UcHealthAuth", description = "SMART on FHIR authorization for UCHealth/Epic")
@RequiredArgsConstructor
public class UcHealthAuthController {

    private final UcHealthOAuthClient oAuthClient;

    @GetMapping("/authorize")
    @Operation(summary = "Redirect the patient to UCHealth/Epic's OAuth login and consent screen")
    public ResponseEntity<Void> authorize() {
        String authorizationUrl = oAuthClient.buildAuthorizationUrl();
        log.info("authorize(): redirecting to Epic authorization endpoint");

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(authorizationUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    /**
     * Epic's registered redirect target. Exchanges the authorization code for a token
     * and stores it. Returns plain text rather than JSON because the patient's browser
     * lands here directly and reads the result.
     */
    @GetMapping("/callback")
    @Operation(summary = "OAuth redirect target - exchanges the authorization code for a stored token")
    public ResponseEntity<String> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription
    ) {
        if (error != null) {
            log.warn("callback(): authorization denied or failed, error={}", error);
            return ResponseEntity.badRequest()
                    .body("Authorization failed: " + error
                            + (errorDescription != null ? " - " + errorDescription : ""));
        }

        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body("Authorization failed: no code returned");
        }

        try {
            UcHealthOAuthToken token = oAuthClient.exchangeAuthorizationCode(code, state);
            log.info("callback(): authorization complete, patientFhirId={}", token.getPatientFhirId());
            return ResponseEntity.ok("Authorization complete. You can close this window.");

        } catch (Exception e) {
            // The message is safe to surface (no code/token in it); the code itself is never echoed.
            log.error("callback(): token exchange failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Authorization failed: " + e.getMessage());
        }
    }
}
