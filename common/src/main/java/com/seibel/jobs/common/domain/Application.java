package com.seibel.jobs.common.domain;

import com.seibel.jobs.common.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Application extends BaseDomain {
    private Long jobPostingId;
    private LocalDate dateApplied;
    private String resumeVersion;
    private ApplicationStatus applicationStatus;
    private String notes;
}
