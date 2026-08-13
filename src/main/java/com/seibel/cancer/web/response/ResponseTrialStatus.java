package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ResponseTrialStatus {
    private String extid;
    private String trialExtid;
    private String patientExtid;
    private String status;
    private String notes;
    private LocalDateTime statusChangedAt;
}
