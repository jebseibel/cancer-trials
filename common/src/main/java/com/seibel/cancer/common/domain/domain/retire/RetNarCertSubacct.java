package com.seibel.cancer.common.domain.domain.retire;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RetNarCertSubacct extends BaseRetCertDomain {

    private String subAccount;
    private String subAccountId;
    private String narId;
    private String asset;
    private String fuelProjectType;
    private String certificateVintage;
    private String certificateSerialNumbers;
    private String quantity;
    private String transferor;
    private String compliancePeriod;
    private String retirementType;
    private String retirementState;
    private String retirementReason;
    private String additionalDetails;
    private String beneficialOwnerName;
    private String retirementDate;
}
