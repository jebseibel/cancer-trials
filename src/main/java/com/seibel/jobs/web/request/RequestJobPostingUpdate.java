package com.seibel.jobs.web.request;

import com.seibel.jobs.common.enums.JobPostingStatus;
import com.seibel.jobs.common.enums.JobSource;
import com.seibel.jobs.common.enums.WorkMode;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
public class RequestJobPostingUpdate extends BaseRequest {

    @Size(max = 255, message = "The title must be at most 255 characters.")
    private String title;

    private Long companyId;

    private String description;

    @Size(max = 120, message = "The city must be at most 120 characters.")
    private String city;

    @Size(max = 120, message = "The state must be at most 120 characters.")
    private String state;

    @Size(max = 120, message = "The country must be at most 120 characters.")
    private String country;

    private WorkMode workMode;

    private Integer salaryMin;

    private Integer salaryMax;

    @Size(max = 8, message = "The salaryCurrency must be at most 8 characters.")
    private String salaryCurrency;

    private JobSource source;

    @Size(max = 1024, message = "The sourceUrl must be at most 1024 characters.")
    private String sourceUrl;

    private LocalDateTime postedAt;

    private JobPostingStatus status;

    private String notes;
}
