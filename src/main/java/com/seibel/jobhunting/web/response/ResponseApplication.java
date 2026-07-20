package com.seibel.jobhunting.web.response;

import com.seibel.jobhunting.common.enums.ApplicationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class ResponseApplication {
    private String extid;
    private Long jobPostingId;
    private LocalDate dateApplied;
    private String resumeVersion;
    private ApplicationStatus applicationStatus;
    private String notes;
}
