package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ResponseStagingRawFhirResource {
    private String extid;
    private String resourceType;
    private String fhirResourceId;
    private String rawPayload;
    private LocalDateTime fetchedAt;
    private LocalDateTime normalizedAt;
    private String normalizationError;
}
