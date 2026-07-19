package com.seibel.jobs.web.request;

import com.seibel.jobs.common.enums.ApplicationStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestApplicationUpdate extends BaseRequest {

    private Long jobPostingId;

    private LocalDate dateApplied;

    @Size(max = 120, message = "The resumeVersion must be at most 120 characters.")
    private String resumeVersion;

    private ApplicationStatus applicationStatus;

    private String notes;
}
