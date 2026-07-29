package com.seibel.cancer.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestOutcomeCreate extends BaseRequest {

    @NotEmpty(message = "The trialExtid is required.")
    private String trialExtid;

    @NotEmpty(message = "The outcomeType is required.")
    @Size(max = 16, message = "The outcomeType must be at most 16 characters.")
    private String outcomeType;

    @NotEmpty(message = "The measure is required.")
    @Size(max = 500, message = "The measure must be at most 500 characters.")
    private String measure;

    private String description;

    @Size(max = 255, message = "The timeFrame must be at most 255 characters.")
    private String timeFrame;
}
