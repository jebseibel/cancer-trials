package com.seibel.cancer.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestTrialStatusCreate extends BaseRequest {

    @NotEmpty(message = "The trialExtid is required.")
    private String trialExtid;

    @NotEmpty(message = "The appUserExtid is required.")
    private String appUserExtid;

    @NotEmpty(message = "The status is required.")
    @Size(max = 16, message = "The status must be at most 16 characters.")
    private String status;

    @Size(max = 65535, message = "The notes must be at most 65535 characters.")
    private String notes;

    private LocalDateTime statusChangedAt;
}
