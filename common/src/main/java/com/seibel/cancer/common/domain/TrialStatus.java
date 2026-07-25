package com.seibel.cancer.common.domain;

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
public class TrialStatus extends BaseDomain {
    private Long trialId;
    private Long appUserId;
    private String status;
    private String notes;
    private LocalDateTime statusChangedAt;
}
