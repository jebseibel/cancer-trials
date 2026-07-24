package com.seibel.cancer.common.domain.domain.retire;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RetErcotTransDetail extends BaseRetCertDomain {

    private String year;
    private String quarter;
    private String fuelType;
    private String facilityId;
    private String serialNumber;
    private String startNum;
    private String endNum;
    private String numOfRecs;
    private String lastOperation;
    private String lastOperationDate;
    private String retireReason;
    private String transferor;
    private String transferee;
    private String transferTransactionStatus;
    private String complianceYear;
    private String memo;
}
