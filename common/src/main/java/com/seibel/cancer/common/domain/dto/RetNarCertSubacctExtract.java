package com.seibel.cancer.common.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for AI extraction of NAR Certificates in Subaccount from PDF.
 * Used by AiService.extractStructuredWithContext() for structured output.
 *
 * The PDF contains a table with certificate details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetNarCertSubacctExtract {

    // Table rows - one per certificate line
    private List<CertSubacctRow> rows;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CertSubacctRow {
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
}
