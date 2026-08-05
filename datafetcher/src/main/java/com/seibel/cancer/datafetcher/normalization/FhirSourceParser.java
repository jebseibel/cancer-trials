package com.seibel.cancer.datafetcher.normalization;

/**
 * Seam for FHIR resource-type-specific parsing, dispatched by
 * staging_raw_fhir_resource.resource_type. Parallel to TrialSourceParser but
 * deliberately NOT the same interface - a FHIR clinical resource and a trial normalize
 * into entirely different shapes.
 */
public interface FhirSourceParser {

    boolean supports(String resourceType);

    NormalizedLabResult parse(String rawPayloadJson);
}
