package com.seibel.cancer.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Transaction extends BaseDomain {
    private String customerExtid;
    private String batchUuid;
    private String documentExtid;
    private String documentName;
    private String trackingSystem;
    private String reportName;
    private String reportType;
    private String uniqueId;
    private String accountOwner;
    private String accountInfo;
    private String transactionDate;
    private String retirementType;
    private String transactionType;
    private String trackingSystemId;
    private String facilityName;
    private String fuelType;
    private String facilityState;
    private String recVintage;
    private Long quantity;
    private String serialNumbers;
    private Integer complianceYear;
    private Integer reportingYear;
    private String lineNumber;
    private String retirementReason;
    private String additionalDetails;
    private String beneficialOwner;
    private String eligibilityStatus;
    private String transferor;
    private String transferee;
    private String transferTransactionStatus;
}
