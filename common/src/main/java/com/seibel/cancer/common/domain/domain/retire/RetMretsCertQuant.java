package com.seibel.cancer.common.domain.domain.retire;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RetMretsCertQuant extends BaseRetCertDomain {

    private String account;
    private String accountId;
    private String fuelType;
    private String vintage;
    private String generationStartDate;
    private String generationEndDate;
    private String mretsId;
    private String unitName;
    private String facilityName;
    private String generatorLocation;
    private String retirementType;
    private String retirementReason;
    private String retiredQuarter;
    private String notes;
    private String reportingPeriod;
    private String retiredFor;
    private String eligibility;
    private String quantityRecs;
    private String serialNumbers;
    private String eTags;
    private String eTagId;
    private String transactionDate;
    private String dateTransactionCompleted;
    private String alternativeEnergyCertificate;
    private String ecologoCertified;
    private String emissionsFreeEnergyCertificate;
}
