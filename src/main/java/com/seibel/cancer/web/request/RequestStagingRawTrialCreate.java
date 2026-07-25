package com.seibel.cancer.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestStagingRawTrialCreate extends BaseRequest {

    @NotNull(message = "The trialSourceId is required.")
    private Long trialSourceId;

    @NotEmpty(message = "The sourceTrialId is required.")
    @Size(max = 255, message = "The sourceTrialId must be at most 255 characters.")
    private String sourceTrialId;

    private String rawPayload;

    @NotNull(message = "The fetchedAt is required.")
    private LocalDateTime fetchedAt;

    private LocalDateTime normalizedAt;

    private String normalizationError;
}
