package com.seibel.cancer.common.domain.domain.retire;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RetMretsTransConfirm extends BaseRetCertDomain {

    private String mretsId;

    // Table columns
    private String account;
    private String rowId;
    private String project;
    private String fuelType;
    private String vintage;
    private String location;
    private String quantity;
    private String serialNumber;

    // Metadata
    private String transactionDate;
    private String mretsOrganization;
    private String retiringRecsCount;
    private String transactionNotes;
    private String retirementReason;
    private String retirementReasonDetails;
    private String toRetirementAccount;
}
