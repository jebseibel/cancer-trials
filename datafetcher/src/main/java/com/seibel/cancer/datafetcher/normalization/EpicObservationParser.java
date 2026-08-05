package com.seibel.cancer.datafetcher.normalization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seibel.cancer.common.domain.LabResult;
import com.seibel.cancer.common.domain.LabResultComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses an Epic FHIR R4 Observation (laboratory category) into a NormalizedLabResult.
 *
 * Field mapping is documented in .claude/epic-tables.md and was confirmed against a real
 * Epic sandbox payload (see src/test/resources/sample-epic-observation.json), not derived
 * from the spec alone. Two things that payload established, and that this parser handles
 * explicitly:
 *   - valueQuantity may carry a value with NO unit.
 *   - code.coding[] holds several codings; LOINC must be selected by system, never by
 *     position.
 */
@Slf4j
@Component
public class EpicObservationParser implements FhirSourceParser {

    private static final String LOINC_SYSTEM = "http://loinc.org";
    private static final String OBSERVATION_CATEGORY_SYSTEM_SUFFIX = "observation-category";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(String resourceType) {
        return "Observation".equals(resourceType);
    }

    @Override
    public NormalizedLabResult parse(String rawPayloadJson) {
        JsonNode root = readTree(rawPayloadJson);

        String fhirResourceId = text(root, "id");
        if (fhirResourceId == null) {
            throw new IllegalArgumentException("Observation payload has no id");
        }

        LabResult labResult = LabResult.builder()
                .fhirResourceId(fhirResourceId)
                .testName(testName(root))
                .loincCode(loincCode(root.path("code")))
                .status(text(root, "status"))
                .category(category(root))
                .effectiveAt(effectiveAt(root))
                .issuedAt(parseDateTime(text(root, "issued")))
                .interpretation(interpretation(root))
                .build();

        applyValue(root, labResult);
        applyReferenceRange(root.path("referenceRange"), labResult);

        List<LabResultComponent> components = parseComponents(root.path("component"));
        labResult.setIsPanel(!components.isEmpty());
        labResult.setDisplayText(buildDisplayText(labResult, components));

        return new NormalizedLabResult(labResult, components);
    }

    private List<LabResultComponent> parseComponents(JsonNode componentArray) {
        List<LabResultComponent> components = new ArrayList<>();
        if (!componentArray.isArray()) {
            return components;
        }

        for (JsonNode node : componentArray) {
            LabResultComponent component = LabResultComponent.builder()
                    .componentName(codeableText(node.path("code")))
                    .loincCode(loincCode(node.path("code")))
                    .interpretation(interpretation(node))
                    .build();

            applyComponentValue(node, component);
            applyComponentReferenceRange(node.path("referenceRange"), component);
            component.setDisplayText(buildComponentDisplayText(component));
            components.add(component);
        }
        return components;
    }

    // ----- value handling -------------------------------------------------------------

    /**
     * FHIR allows value[x] in several shapes. Numeric goes to valueQuantity; everything
     * else is rendered into valueString so nothing is silently dropped.
     */
    private void applyValue(JsonNode node, LabResult target) {
        JsonNode quantity = node.path("valueQuantity");
        if (!quantity.isMissingNode()) {
            target.setValueQuantity(decimal(quantity.path("value")));
            // May legitimately be absent - Epic returned {"value": 5.1} with no unit.
            target.setValueUnit(text(quantity, "unit"));
            return;
        }
        target.setValueString(nonQuantityValue(node));
    }

    private void applyComponentValue(JsonNode node, LabResultComponent target) {
        JsonNode quantity = node.path("valueQuantity");
        if (!quantity.isMissingNode()) {
            target.setValueQuantity(decimal(quantity.path("value")));
            target.setValueUnit(text(quantity, "unit"));
            return;
        }
        target.setValueString(nonQuantityValue(node));
    }

    /** Renders whichever non-quantity value[x] variant is present, or null if none. */
    private String nonQuantityValue(JsonNode node) {
        String valueString = text(node, "valueString");
        if (valueString != null) {
            return valueString;
        }
        JsonNode codeable = node.path("valueCodeableConcept");
        if (!codeable.isMissingNode()) {
            return codeableText(codeable);
        }
        if (node.hasNonNull("valueBoolean")) {
            return Boolean.toString(node.path("valueBoolean").asBoolean());
        }
        if (node.hasNonNull("valueInteger")) {
            return Integer.toString(node.path("valueInteger").asInt());
        }
        JsonNode range = node.path("valueRange");
        if (!range.isMissingNode()) {
            String low = quantityWithUnit(range.path("low"));
            String high = quantityWithUnit(range.path("high"));
            return (low == null ? "?" : low) + " - " + (high == null ? "?" : high);
        }
        return null;
    }

    private void applyReferenceRange(JsonNode rangeArray, LabResult target) {
        JsonNode range = firstElement(rangeArray);
        if (range == null) {
            return;
        }
        target.setReferenceRangeLow(decimal(range.path("low").path("value")));
        target.setReferenceRangeHigh(decimal(range.path("high").path("value")));
        target.setReferenceRangeText(text(range, "text"));
    }

    private void applyComponentReferenceRange(JsonNode rangeArray, LabResultComponent target) {
        JsonNode range = firstElement(rangeArray);
        if (range == null) {
            return;
        }
        target.setReferenceRangeLow(decimal(range.path("low").path("value")));
        target.setReferenceRangeHigh(decimal(range.path("high").path("value")));
        target.setReferenceRangeText(text(range, "text"));
    }

    // ----- field extraction ------------------------------------------------------------

    private String testName(JsonNode root) {
        String name = codeableText(root.path("code"));
        return name != null ? name : "Unknown test";
    }

    /** code.text, falling back to the LOINC coding's display, then any coding's display. */
    private String codeableText(JsonNode codeable) {
        String text = text(codeable, "text");
        if (text != null) {
            return text;
        }
        JsonNode loinc = codingBySystem(codeable.path("coding"), LOINC_SYSTEM);
        if (loinc != null) {
            String display = text(loinc, "display");
            if (display != null) {
                return display;
            }
        }
        JsonNode first = firstElement(codeable.path("coding"));
        return first == null ? null : text(first, "display");
    }

    /**
     * Selects the LOINC coding by system rather than by position - Epic returns both a
     * LOINC coding and an internal OID coding, and their order is not guaranteed.
     */
    private String loincCode(JsonNode codeable) {
        JsonNode loinc = codingBySystem(codeable.path("coding"), LOINC_SYSTEM);
        return loinc == null ? null : text(loinc, "code");
    }

    private String category(JsonNode root) {
        for (JsonNode categoryNode : root.path("category")) {
            for (JsonNode coding : categoryNode.path("coding")) {
                String system = text(coding, "system");
                if (system != null && system.endsWith(OBSERVATION_CATEGORY_SYSTEM_SUFFIX)) {
                    return text(coding, "code");
                }
            }
        }
        // No standard category coding - fall back to the first category's text.
        JsonNode first = firstElement(root.path("category"));
        return first == null ? null : text(first, "text");
    }

    /** effectiveDateTime, falling back to effectivePeriod.start. */
    private LocalDateTime effectiveAt(JsonNode root) {
        String effective = text(root, "effectiveDateTime");
        if (effective == null) {
            effective = text(root.path("effectivePeriod"), "start");
        }
        return parseDateTime(effective);
    }

    private String interpretation(JsonNode node) {
        JsonNode first = firstElement(node.path("interpretation"));
        return first == null ? null : codeableText(first);
    }

    // ----- display text ----------------------------------------------------------------

    /**
     * A readable one-line rendering of the row. Exists because the end goal is RAG over
     * the record - prose embeds far better than reassembled columns.
     */
    private String buildDisplayText(LabResult labResult, List<LabResultComponent> components) {
        StringBuilder sb = new StringBuilder(labResult.getTestName());

        String value = renderValue(labResult.getValueQuantity(), labResult.getValueUnit(),
                labResult.getValueString());
        if (value != null) {
            sb.append(": ").append(value);
        }
        if (labResult.getInterpretation() != null) {
            sb.append(" (").append(labResult.getInterpretation()).append(")");
        }
        String range = renderRange(labResult.getReferenceRangeLow(), labResult.getReferenceRangeHigh(),
                labResult.getReferenceRangeText());
        if (range != null) {
            sb.append(", reference range ").append(range);
        }
        if (labResult.getEffectiveAt() != null) {
            sb.append(", collected ").append(labResult.getEffectiveAt().toLocalDate());
        }
        if (labResult.getStatus() != null) {
            sb.append(", status ").append(labResult.getStatus());
        }
        if (!components.isEmpty()) {
            sb.append(". Components: ");
            for (int i = 0; i < components.size(); i++) {
                if (i > 0) {
                    sb.append("; ");
                }
                sb.append(components.get(i).getDisplayText());
            }
        }
        return sb.append('.').toString();
    }

    private String buildComponentDisplayText(LabResultComponent component) {
        StringBuilder sb = new StringBuilder(
                component.getComponentName() == null ? "Unknown component" : component.getComponentName());

        String value = renderValue(component.getValueQuantity(), component.getValueUnit(),
                component.getValueString());
        if (value != null) {
            sb.append(": ").append(value);
        }
        if (component.getInterpretation() != null) {
            sb.append(" (").append(component.getInterpretation()).append(")");
        }
        String range = renderRange(component.getReferenceRangeLow(), component.getReferenceRangeHigh(),
                component.getReferenceRangeText());
        if (range != null) {
            sb.append(", ref ").append(range);
        }
        return sb.toString();
    }

    /** Value plus unit when both exist; value alone when Epic omitted the unit. */
    private String renderValue(BigDecimal quantity, String unit, String valueString) {
        if (quantity != null) {
            String rendered = quantity.stripTrailingZeros().toPlainString();
            return unit == null ? rendered : rendered + " " + unit;
        }
        return valueString;
    }

    private String renderRange(BigDecimal low, BigDecimal high, String text) {
        if (text != null) {
            return text;
        }
        if (low == null && high == null) {
            return null;
        }
        String lowText = low == null ? "?" : low.stripTrailingZeros().toPlainString();
        String highText = high == null ? "?" : high.stripTrailingZeros().toPlainString();
        return lowText + "-" + highText;
    }

    private String quantityWithUnit(JsonNode quantity) {
        BigDecimal value = decimal(quantity.path("value"));
        if (value == null) {
            return null;
        }
        String unit = text(quantity, "unit");
        String rendered = value.stripTrailingZeros().toPlainString();
        return unit == null ? rendered : rendered + " " + unit;
    }

    // ----- json helpers ----------------------------------------------------------------

    private JsonNode readTree(String rawPayloadJson) {
        try {
            return objectMapper.readTree(rawPayloadJson);
        } catch (Exception e) {
            throw new IllegalArgumentException("Observation payload is not valid JSON", e);
        }
    }

    private JsonNode codingBySystem(JsonNode codingArray, String system) {
        for (JsonNode coding : codingArray) {
            if (system.equals(text(coding, "system"))) {
                return coding;
            }
        }
        return null;
    }

    private JsonNode firstElement(JsonNode array) {
        return array.isArray() && !array.isEmpty() ? array.get(0) : null;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text.isBlank() ? null : text;
    }

    private BigDecimal decimal(JsonNode node) {
        return node.isMissingNode() || node.isNull() ? null : node.decimalValue();
    }

    /**
     * FHIR instants carry an offset ("2019-05-28T14:22:00Z"); plain dateTimes may not.
     * An unparseable value yields null rather than failing the whole row.
     */
    private LocalDateTime parseDateTime(String value) {
        if (value == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // Not offset-qualified - fall through to a local parse.
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            log.warn("parseDateTime(): unparseable FHIR dateTime '{}'", value);
            return null;
        }
    }
}
