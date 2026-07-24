package com.seibel.cancer.common.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for AI extraction of NAR Voluntary Compliance Report from PDF.
 * Used by AiService.extractStructuredWithContext() for structured output.
 *
 * The PDF contains a table with retirement compliance details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetNarVolComplyExtract {

    // Table rows - one per retirement record
    private List<VolComplyRow> rows;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VolComplyRow {
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
}
