package com.seibel.cancer.web.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestTrialStatusUpdate extends BaseRequest {

    private String trialExtid;

    private String patientExtid;

    @Size(max = 16, message = "The status must be at most 16 characters.")
    private String status;

    @Size(max = 65535, message = "The notes must be at most 65535 characters.")
    private String notes;

    private LocalDateTime statusChangedAt;
}
