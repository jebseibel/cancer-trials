package com.seibel.cancer.datafetcher.normalization;

import com.seibel.cancer.common.domain.LabResult;
import com.seibel.cancer.common.domain.LabResultComponent;

import java.util.List;

/**
 * A parsed FHIR Observation: the parent lab result plus its component rows (empty for a
 * simple single-value lab, populated for a panel). The components carry no labResultId
 * yet - the normalizer sets it once the parent row has an id.
 *
 * Deliberately not a NormalizedTrial: a patient's clinical record and a trial are
 * different shapes, and forcing them into one carrier would help nobody.
 */
public record NormalizedLabResult(LabResult labResult, List<LabResultComponent> components) {

    public NormalizedLabResult {
        components = components == null ? List.of() : List.copyOf(components);
    }
}
