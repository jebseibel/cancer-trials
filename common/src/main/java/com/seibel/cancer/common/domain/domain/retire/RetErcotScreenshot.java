package com.seibel.cancer.common.domain.domain.retire;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RetErcotScreenshot extends BaseRetCertDomain {

    private String year;
    private String quarter;
    private String recType;
    private String facilityId;
    private String serialNumber;
    private String serialStart;
    private String serialEnd;
    private String quantity;
    private String action;
    private String fromEntity;
    private String toEntity;
    private String date;
    private String retireReason;
    private String transferor;
    private String transferee;
    private String transferTransactionStatus;
    private String complianceYear;
    private String memo;
}
