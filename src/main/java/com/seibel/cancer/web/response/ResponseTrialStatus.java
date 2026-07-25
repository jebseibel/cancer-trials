package com.seibel.cancer.web.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ResponseTrialStatus {
    private String extid;
    private Long trialId;
    private Long appUserId;
    private String status;
    private String notes;
    private LocalDateTime statusChangedAt;
}
