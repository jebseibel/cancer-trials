package com.seibel.jobs.web.request;

import com.seibel.jobs.common.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestApplicationCreate extends BaseRequest {

    @NotNull(message = "The jobPostingId is required.")
    private Long jobPostingId;

    @NotNull(message = "The dateApplied is required.")
    private LocalDate dateApplied;

    @Size(max = 120, message = "The resumeVersion must be at most 120 characters.")
    private String resumeVersion;

    @NotNull(message = "The applicationStatus is required.")
    private ApplicationStatus applicationStatus;

    private String notes;
}
