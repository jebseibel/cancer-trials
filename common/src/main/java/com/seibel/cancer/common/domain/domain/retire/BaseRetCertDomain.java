package com.seibel.cancer.common.domain.domain.retire;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class BaseRetCertDomain extends BaseRetDomain {

    private String createSerialNumber;
    private String eligibilityStatus;
    private Long uploadId;
    private LocalDateTime effectiveDate;
    private Boolean isSixMonthsBefore;
    private Boolean isThreeMonthsAfter;
    private String lineNumber;
}
