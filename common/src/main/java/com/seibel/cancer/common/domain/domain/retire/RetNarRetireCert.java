package com.seibel.cancer.common.domain.domain.retire;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RetNarRetireCert extends BaseRetCertDomain {


    // Metadata from header
    private String beneficiary;
    private String totalRecsRetired;
    private String retiringAccountHolder;
    private String retirementReasonDetails;
    private String retirementDate;

    // Row data from table
    private String narId;
    private String projectName;
    private String projectType;
    private String narSerialNumbers;
    private String quantity;
}
