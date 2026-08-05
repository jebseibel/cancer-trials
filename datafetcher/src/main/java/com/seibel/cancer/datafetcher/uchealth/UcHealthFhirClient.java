package com.seibel.cancer.datafetcher.uchealth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seibel.cancer.common.domain.UcHealthOAuthToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Authenticated FHIR R4 client for UCHealth/Epic. Every call carries the stored access
 * token as a Bearer header, refreshing it first if it's near expiry.
 *
 * Search results come back as a FHIR Bundle paginated by a "next" link - a different
 * shape from CT.gov's pageToken, so pagination follows the absolute URL Epic hands back
 * rather than building the next URI itself.
 *
 * Note: MedicationStatement is deliberately absent. Epic exposes it only in DSTU2/STU3,
 * not R4 - see .claude/UCHEALTH_INGESTION_PLAN.md.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UcHealthFhirClient {

    /** Stop following next-links past this, so a runaway Bundle can't loop forever. */
    private static final int MAX_PAGES = 50;

    public static final String MEDICATION_REQUEST = "MedicationRequest";
    public static final String OBSERVATION = "Observation";

    private final UcHealthOAuthProperties properties;
    private final UcHealthOAuthClient oAuthClient;

    private final RestClient restClient = RestClient.builder().build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Fetches every MedicationRequest for the authorized patient, following Bundle
     * pagination. Returns one JsonNode per resource, exactly as Epic returned it -
     * callers preserve this verbatim in staging.
     */
    public List<JsonNode> fetchMedicationRequests() {
        UcHealthOAuthToken token = oAuthClient.ensureValidToken();
        String patientFhirId = requirePatientId(token);

        String url = UriComponentsBuilder.fromUriString(fhirBaseUrl())
                .pathSegment(MEDICATION_REQUEST)
                .queryParam("patient", patientFhirId)
                .encode()
                .build()
                .toUriString();

        List<JsonNode> resources = searchAllPages(url, token.getAccessToken(), MEDICATION_REQUEST);
        log.info("fetchMedicationRequests(): patient={}, fetched={}", patientFhirId, resources.size());
        return resources;
    }

    /**
     * Fetches the authorized patient's laboratory Observations (test results), following
     * Bundle pagination. Epic requires the category filter here - an unfiltered Observation
     * search is rejected.
     */
    public List<JsonNode> fetchLabObservations() {
        UcHealthOAuthToken token = oAuthClient.ensureValidToken();
        String patientFhirId = requirePatientId(token);

        String url = UriComponentsBuilder.fromUriString(fhirBaseUrl())
                .pathSegment(OBSERVATION)
                .queryParam("patient", patientFhirId)
                .queryParam("category", "laboratory")
                .encode()
                .build()
                .toUriString();

        List<JsonNode> resources = searchAllPages(url, token.getAccessToken(), OBSERVATION);
        log.info("fetchLabObservations(): patient={}, fetched={}", patientFhirId, resources.size());
        return resources;
    }

    /**
     * Walks a FHIR search Bundle and its next-links, collecting each entry's resource.
     * The next link is an absolute URL supplied by the server, so it's followed as-is.
     */
    private List<JsonNode> searchAllPages(String startUrl, String accessToken, String expectedResourceType) {
        List<JsonNode> resources = new ArrayList<>();
        String url = startUrl;

        for (int page = 0; page < MAX_PAGES && url != null; page++) {
            JsonNode bundle = get(url, accessToken);
            if (bundle == null) {
                break;
            }

            for (JsonNode entry : bundle.path("entry")) {
                JsonNode resource = entry.path("resource");
                if (resource.isMissingNode() || resource.isNull()) {
                    continue;
                }
                // Epic appends an OperationOutcome entry to search Bundles (a "this may not be
                // the complete record" warning). It is not data - staging it would create a
                // bogus row. Keep only entries of the type actually being searched for.
                String resourceType = resource.path("resourceType").asText(null);
                if (!expectedResourceType.equals(resourceType)) {
                    log.debug("searchAllPages(): skipping non-{} entry of type {}",
                            expectedResourceType, resourceType);
                    continue;
                }
                resources.add(resource);
            }

            url = nextPageUrl(bundle);
        }
        return resources;
    }

    /** Bundle.link[relation=next].url, or null when this is the last page. */
    private String nextPageUrl(JsonNode bundle) {
        for (JsonNode link : bundle.path("link")) {
            if ("next".equals(link.path("relation").asText())) {
                String url = link.path("url").asText(null);
                if (url != null && !url.isBlank()) {
                    return url;
                }
            }
        }
        return null;
    }

    private JsonNode get(String url, String accessToken) {
        try {
            return restClient.get()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.ACCEPT, "application/fhir+json")
                    .retrieve()
                    .body(JsonNode.class);

        } catch (Exception e) {
            // Never log the Authorization header - the URL alone is enough to debug with.
            log.error("get(): FHIR request failed, url={}", url, e);
            throw new IllegalStateException("FHIR request failed: " + e.getMessage(), e);
        }
    }

    private String requirePatientId(UcHealthOAuthToken token) {
        String patientFhirId = token.getPatientFhirId();
        if (patientFhirId == null || patientFhirId.isBlank()) {
            throw new IllegalStateException(
                    "Stored token has no patient FHIR id - re-authorize with the launch/patient scope");
        }
        return patientFhirId;
    }

    private String fhirBaseUrl() {
        String baseUrl = properties.getFhirBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("uchealth.fhir-base-url is not configured (see .env)");
        }
        return baseUrl;
    }

    public String toRawJson(JsonNode resource) {
        try {
            return objectMapper.writeValueAsString(resource);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize FHIR resource JSON", e);
        }
    }

    /** The resource's own FHIR id - the dedup key, analogous to CT.gov's nctId. */
    public String extractResourceId(JsonNode resource) {
        return resource.path("id").asText(null);
    }
}
