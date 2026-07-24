package com.seibel.cancer.common.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for AI extraction of WREGIS Transaction Details from PDF.
 * Used by AiService.extractStructuredWithContext() for structured output.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetWregisTransDetailsExtract {

    private List<TransactionDetail> transactions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionDetail {
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
        private String wregisGuid;
        private String wregisStatus;
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
}
