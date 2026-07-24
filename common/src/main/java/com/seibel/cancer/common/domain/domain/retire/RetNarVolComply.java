package com.seibel.cancer.common.domain.domain.retire;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RetNarVolComply extends BaseRetCertDomain {

    private String accountHolder;
    private String subAccount;
    private String subAccountId;
    private String retirementType;
    private String state;
    private String compliancePeriod;
    private String reason;
    private String additionalDetails;
    private String beneficialOwnerName;
    private String narId;
    private String asset;
    private String fuelProjectType;
    private String certificateVintage;
    private String certificateSerialNumbers;
    private String quantity;
    private String complianceEquivalency;
    private String durationStartDate;
    private String durationEndDate;
    private String crsListedStatus;
    private String crsListedStartDate;
    private String crsListedExpirationDate;
    private String crsListedEndDateExtendedUse;
    private String retirementDate;
}
