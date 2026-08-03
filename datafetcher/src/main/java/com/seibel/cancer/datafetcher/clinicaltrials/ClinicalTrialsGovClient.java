package com.seibel.cancer.datafetcher.clinicaltrials;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Thin REST client for ClinicalTrials.gov v2's GET /studies endpoint. No API key
 * required; no documented hard rate limit, but this pages conservatively.
 */
@Slf4j
@Component
public class ClinicalTrialsGovClient {

    private static final String BASE_URL = "https://clinicaltrials.gov/api/v2";
    private static final int PAGE_SIZE = 100;

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClinicalTrialsGovClient() {
        this.restClient = RestClient.builder().baseUrl(BASE_URL).build();
    }

    /**
     * Fetches up to maxStudies studies matching the given search params, paging as
     * needed. Returns each study's raw JSON node as returned by the API (one entry
     * per study) - callers preserve this verbatim in staging.
     */
    public List<JsonNode> searchStudies(String condition, String term, String location, int maxStudies) {
        List<JsonNode> studies = new ArrayList<>();
        String pageToken = null;

        while (studies.size() < maxStudies) {
            int remaining = maxStudies - studies.size();
            int pageSize = Math.min(PAGE_SIZE, remaining);

            String pageTokenFinal = pageToken;
            JsonNode page = restClient.get()
                    .uri(uriBuilder -> buildSearchUri(uriBuilder, condition, term, location, pageSize, pageTokenFinal))
                    .retrieve()
                    .body(JsonNode.class);

            if (page == null) {
                break;
            }

            JsonNode pageStudies = page.path("studies");
            if (!pageStudies.isArray() || pageStudies.isEmpty()) {
                break;
            }
            pageStudies.forEach(studies::add);

            JsonNode nextPageToken = page.path("nextPageToken");
            if (nextPageToken.isMissingNode() || nextPageToken.isNull()) {
                break;
            }
            pageToken = nextPageToken.asText();
        }

        log.info("searchStudies(): condition={}, term={}, location={}, fetched={}",
                condition, term, location, studies.size());
        return studies.size() > maxStudies ? studies.subList(0, maxStudies) : studies;
    }

    private URI buildSearchUri(UriBuilder uriBuilder, String condition, String term, String location,
                                int pageSize, String pageToken) {
        uriBuilder = uriBuilder.path("/studies").queryParam("pageSize", pageSize);
        if (StringUtils.hasText(condition)) {
            uriBuilder = uriBuilder.queryParam("query.cond", condition);
        }
        if (StringUtils.hasText(term)) {
            uriBuilder = uriBuilder.queryParam("query.term", term);
        }
        if (StringUtils.hasText(location)) {
            uriBuilder = uriBuilder.queryParam("query.locn", location);
        }
        if (StringUtils.hasText(pageToken)) {
            uriBuilder = uriBuilder.queryParam("pageToken", pageToken);
        }
        return uriBuilder.build();
    }

    public String toRawJson(JsonNode study) {
        try {
            return objectMapper.writeValueAsString(study);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize study JSON", e);
        }
    }

    public String extractNctId(JsonNode study) {
        return study.path("protocolSection").path("identificationModule").path("nctId").asText(null);
    }
}
