package com.seibel.cancer.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestStagingRawFhirResourceCreate extends BaseRequest {

    @NotEmpty(message = "The resourceType is required.")
    @Size(max = 64, message = "The resourceType must be at most 64 characters.")
    private String resourceType;

    @NotEmpty(message = "The fhirResourceId is required.")
    @Size(max = 255, message = "The fhirResourceId must be at most 255 characters.")
    private String fhirResourceId;

    private String rawPayload;

    @NotNull(message = "The fetchedAt is required.")
    private LocalDateTime fetchedAt;

    private LocalDateTime normalizedAt;

    private String normalizationError;
}
