package com.seibel.jobhunting.web.response;

import com.seibel.jobhunting.common.enums.JobPostingStatus;
import com.seibel.jobhunting.common.enums.JobSource;
import com.seibel.jobhunting.common.enums.WorkMode;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ResponseJobPosting {
    private String extid;
    private String title;
    private Long companyId;
    private String description;
    private String city;
    private String state;
    private String country;
    private WorkMode workMode;
    private Integer salaryMin;
    private Integer salaryMax;
    private String salaryCurrency;
    private JobSource source;
    private String sourceUrl;
    private LocalDateTime postedAt;
    private JobPostingStatus status;
    private String notes;
}
