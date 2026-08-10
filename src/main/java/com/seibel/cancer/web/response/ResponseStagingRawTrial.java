package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ResponseStagingRawTrial {
    private String extid;
    private Long trialSourceId;
    private String sourceTrialId;
    private String rawPayload;
    private String payloadHash;
    private LocalDateTime fetchedAt;
    private LocalDateTime normalizedAt;
    private String normalizationError;
}
