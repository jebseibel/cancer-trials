package com.seibel.cancer.web.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestStagingRawFhirResourceUpdate extends BaseRequest {

    @Size(max = 64, message = "The resourceType must be at most 64 characters.")
    private String resourceType;

    @Size(max = 255, message = "The fhirResourceId must be at most 255 characters.")
    private String fhirResourceId;

    private String rawPayload;

    private LocalDateTime fetchedAt;

    private LocalDateTime normalizedAt;

    private String normalizationError;
}
