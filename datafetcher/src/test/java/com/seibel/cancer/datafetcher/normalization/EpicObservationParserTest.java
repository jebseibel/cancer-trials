package com.seibel.cancer.datafetcher.normalization;

import com.seibel.cancer.common.domain.LabResult;
import com.seibel.cancer.common.domain.LabResultComponent;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fixture-based, using the REAL Epic sandbox payload captured from Camila Lopez's record
 * (sample-epic-observation.json) plus hand-built panel payloads for cases the sandbox
 * doesn't contain. Same pattern as ClinicalTrialsGovParserTest.
 */
class EpicObservationParserTest {

    private final EpicObservationParser parser = new EpicObservationParser();

    private String sampleObservationJson() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/sample-epic-observation.json")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void supports_onlyObservation() {
        assertThat(parser.supports("Observation")).isTrue();
        assertThat(parser.supports("MedicationRequest")).isFalse();
        assertThat(parser.supports(null)).isFalse();
    }

    @Test
    void parse_realEpicPayload_mapsEveryField() throws Exception {
        NormalizedLabResult normalized = parser.parse(sampleObservationJson());
        LabResult result = normalized.labResult();

        assertThat(result.getFhirResourceId())
                .isEqualTo("eyPMWgv2u2RUfsV4p1lLKuUtqyPs2-QNi2zKvbTsFYtRByc6B.cSi1iVU5V2HOpX23");
        assertThat(result.getTestName()).isEqualTo("Hemoglobin A1C");
        assertThat(result.getStatus()).isEqualTo("final");
        assertThat(result.getCategory()).isEqualTo("laboratory");
        assertThat(result.getValueQuantity()).isEqualByComparingTo(new BigDecimal("5.1"));
        assertThat(result.getEffectiveAt()).isNotNull();
        assertThat(result.getIssuedAt()).isNotNull();
        assertThat(result.getIsPanel()).isFalse();
        assertThat(normalized.components()).isEmpty();
    }

    @Test
    void parse_selectsLoincCodeBySystem_notByPosition() throws Exception {
        // The real payload carries LOINC *and* an Epic-internal OID coding. Taking
        // coding[0] positionally would be wrong the moment Epic reorders them.
        LabResult result = parser.parse(sampleObservationJson()).labResult();

        assertThat(result.getLoincCode()).isEqualTo("4548-4");
    }

    @Test
    void parse_leavesValueUnitNull_whenEpicOmitsIt() throws Exception {
        // Epic returned {"value": 5.1} with no unit at all.
        LabResult result = parser.parse(sampleObservationJson()).labResult();

        assertThat(result.getValueUnit()).isNull();
        assertThat(result.getValueQuantity()).isNotNull();
    }

    @Test
    void parse_buildsReadableDisplayText() throws Exception {
        LabResult result = parser.parse(sampleObservationJson()).labResult();

        // No unit, so the value renders bare rather than with a dangling "null".
        assertThat(result.getDisplayText())
                .isEqualTo("Hemoglobin A1C: 5.1, collected 2019-05-28, status final.");
    }

    @Test
    void parse_panelPayload_producesComponentRows() {
        String cbc = """
                {
                  "resourceType": "Observation",
                  "id": "panel-1",
                  "status": "final",
                  "code": { "text": "CBC" },
                  "effectiveDateTime": "2026-01-15T09:30:00Z",
                  "component": [
                    {
                      "code": { "text": "Hemoglobin",
                                "coding": [{"system":"http://loinc.org","code":"718-7"}] },
                      "valueQuantity": { "value": 13.2, "unit": "g/dL" },
                      "referenceRange": [{ "low": {"value": 12.0}, "high": {"value": 16.0} }]
                    },
                    {
                      "code": { "text": "Platelets" },
                      "valueQuantity": { "value": 250, "unit": "10*3/uL" },
                      "interpretation": [{ "text": "Normal" }]
                    }
                  ]
                }
                """;

        NormalizedLabResult normalized = parser.parse(cbc);

        assertThat(normalized.labResult().getIsPanel()).isTrue();
        assertThat(normalized.components()).hasSize(2);

        LabResultComponent hemoglobin = normalized.components().get(0);
        assertThat(hemoglobin.getComponentName()).isEqualTo("Hemoglobin");
        assertThat(hemoglobin.getLoincCode()).isEqualTo("718-7");
        assertThat(hemoglobin.getValueQuantity()).isEqualByComparingTo(new BigDecimal("13.2"));
        assertThat(hemoglobin.getValueUnit()).isEqualTo("g/dL");
        assertThat(hemoglobin.getReferenceRangeLow()).isEqualByComparingTo(new BigDecimal("12.0"));
        assertThat(hemoglobin.getDisplayText()).isEqualTo("Hemoglobin: 13.2 g/dL, ref 12-16");

        assertThat(normalized.components().get(1).getInterpretation()).isEqualTo("Normal");
        // The panel's own summary folds in its components, so one chunk carries the whole panel.
        assertThat(normalized.labResult().getDisplayText()).contains("Components: Hemoglobin: 13.2 g/dL");
    }

    @Test
    void parse_nonNumericValue_goesToValueString() {
        String culture = """
                {
                  "resourceType": "Observation",
                  "id": "obs-culture",
                  "status": "final",
                  "code": { "text": "Blood Culture" },
                  "valueString": "No growth after 5 days"
                }
                """;

        LabResult result = parser.parse(culture).labResult();

        assertThat(result.getValueString()).isEqualTo("No growth after 5 days");
        assertThat(result.getValueQuantity()).isNull();
    }

    @Test
    void parse_codeableConceptValue_isRenderedRatherThanDropped() {
        String payload = """
                {
                  "resourceType": "Observation",
                  "id": "obs-codeable",
                  "status": "final",
                  "code": { "text": "HIV Screen" },
                  "valueCodeableConcept": { "text": "Non-reactive" }
                }
                """;

        assertThat(parser.parse(payload).labResult().getValueString()).isEqualTo("Non-reactive");
    }

    @Test
    void parse_fallsBackToEffectivePeriodStart_whenNoEffectiveDateTime() {
        String payload = """
                {
                  "resourceType": "Observation",
                  "id": "obs-period",
                  "status": "final",
                  "code": { "text": "Timed Collection" },
                  "effectivePeriod": { "start": "2026-02-01T08:00:00Z", "end": "2026-02-02T08:00:00Z" }
                }
                """;

        assertThat(parser.parse(payload).labResult().getEffectiveAt())
                .isEqualTo("2026-02-01T08:00");
    }

    @Test
    void parse_missingId_isRejected() {
        String payload = """
                { "resourceType": "Observation", "status": "final", "code": { "text": "X" } }
                """;

        assertThatThrownBy(() -> parser.parse(payload))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no id");
    }

    @Test
    void parse_invalidJson_isRejected() {
        assertThatThrownBy(() -> parser.parse("not json at all"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not valid JSON");
    }
}
