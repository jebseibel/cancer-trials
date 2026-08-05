package com.seibel.cancer.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StagingRawFhirResource extends BaseDomain {
    private String resourceType;
    private String fhirResourceId;
    private String rawPayload;
    private LocalDateTime fetchedAt;
    private LocalDateTime normalizedAt;
    private String normalizationError;
}
