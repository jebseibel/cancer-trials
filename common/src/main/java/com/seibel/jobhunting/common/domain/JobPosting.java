package com.seibel.jobhunting.common.domain;

import com.seibel.jobhunting.common.enums.JobPostingStatus;
import com.seibel.jobhunting.common.enums.JobSource;
import com.seibel.jobhunting.common.enums.WorkMode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class JobPosting extends BaseDomain {
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
