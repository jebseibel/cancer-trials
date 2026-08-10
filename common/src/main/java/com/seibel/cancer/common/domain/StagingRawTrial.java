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
public class StagingRawTrial extends BaseDomain {
    private Long trialSourceId;
    private String sourceTrialId;
    private String rawPayload;
    private String payloadHash;
    private LocalDateTime fetchedAt;
    private LocalDateTime normalizedAt;
    private String normalizationError;
}
