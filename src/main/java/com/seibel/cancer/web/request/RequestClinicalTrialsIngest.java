package com.seibel.cancer.web.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestClinicalTrialsIngest extends BaseRequest {

    /** Maps to ClinicalTrials.gov's query.cond - condition/disease search term. */
    private String condition;

    /** Maps to query.term - free-text search term. */
    private String term;

    /** Maps to query.locn - location search term. */
    private String location;

    @Min(value = 1, message = "The maxStudies must be at least 1.")
    @Max(value = 500, message = "The maxStudies must be at most 500.")
    private Integer maxStudies = 50;
}
