package com.seibel.cancer.web.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestStagingRawTrialUpdate extends BaseRequest {

    private Long trialSourceId;

    @Size(max = 255, message = "The sourceTrialId must be at most 255 characters.")
    private String sourceTrialId;

    private String rawPayload;

    private LocalDateTime fetchedAt;

    private LocalDateTime normalizedAt;

    private String normalizationError;
}
