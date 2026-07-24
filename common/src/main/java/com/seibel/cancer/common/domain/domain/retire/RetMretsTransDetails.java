package com.seibel.cancer.common.domain.domain.retire;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RetMretsTransDetails extends BaseRetCertDomain {

    private String dateStarted;
    private String dateEnded;
    private String sourceOrganization;
    private String destinationOrganization;
    private String transactionType;
    private String sourceAccount;
    private String destinationAccount;
    private String fuelSources;
    private String fuelType;
    private String retirementType;
    private String retirementReason;
    private String notes;
    private String retiredTo;
    private String compliancePeriod;
    private String mretsId;
    private String unitName;
    private String facilityName;
    private String location;
    private String eligibility;
    private String vintage;
    private String generationStartDate;
    private String generationEndDate;
    private String serialNumbers;
    private String quantityRecs;
    private String retirementCategory;
}
