package com.seibel.cancer.datafetcher.uchealth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Asserts against a real Epic sandbox Observation payload
 * (src/test/resources/sample-epic-observation.json, captured from Camila Lopez's
 * sandbox record) rather than a spec-derived guess.
 */
class UcHealthFhirClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonNode sampleObservation() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/sample-epic-observation.json")) {
            return OBJECT_MAPPER.readTree(in);
        }
    }

    @Test
    void extractResourceId_readsTheFhirId() throws Exception {
        UcHealthFhirClient client = new UcHealthFhirClient(new UcHealthOAuthProperties(), null);

        String id = client.extractResourceId(sampleObservation());

        assertThat(id).isEqualTo(
                "eyPMWgv2u2RUfsV4p1lLKuUtqyPs2-QNi2zKvbTsFYtRByc6B.cSi1iVU5V2HOpX23");
    }

    @Test
    void toRawJson_roundTripsThePayload() throws Exception {
        UcHealthFhirClient client = new UcHealthFhirClient(new UcHealthOAuthProperties(), null);
        JsonNode observation = sampleObservation();

        String raw = client.toRawJson(observation);

        // Staging keeps the payload verbatim - re-parsing must yield the same tree.
        assertThat(OBJECT_MAPPER.readTree(raw)).isEqualTo(observation);
    }

    @Test
    void sampleObservation_hasTheShapeTheParserWillRelyOn() throws Exception {
        JsonNode observation = sampleObservation();

        assertThat(observation.path("resourceType").asText()).isEqualTo("Observation");
        assertThat(observation.path("status").asText()).isEqualTo("final");
        assertThat(observation.path("code").path("text").asText()).isEqualTo("Hemoglobin A1C");
        assertThat(observation.path("effectiveDateTime").asText()).isEqualTo("2019-05-28T14:22:00Z");

        // Epic returned valueQuantity with a value but NO unit - the parser must treat
        // unit as optional rather than assuming value and unit travel together.
        assertThat(observation.path("valueQuantity").path("value").asDouble()).isEqualTo(5.1);
        assertThat(observation.path("valueQuantity").has("unit")).isFalse();

        // Two codings: LOINC plus an Epic-internal OID. The parser must select LOINC by
        // system rather than taking coding[0] positionally.
        JsonNode codings = observation.path("code").path("coding");
        assertThat(codings).hasSize(2);
        boolean hasLoinc = false;
        for (JsonNode coding : codings) {
            if ("http://loinc.org".equals(coding.path("system").asText())) {
                hasLoinc = true;
                assertThat(coding.path("code").asText()).isEqualTo("4548-4");
            }
        }
        assertThat(hasLoinc).isTrue();
    }
}
