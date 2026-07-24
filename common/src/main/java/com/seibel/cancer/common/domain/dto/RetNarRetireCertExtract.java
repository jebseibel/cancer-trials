package com.seibel.cancer.common.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for AI extraction of NAR Certificate of Retirement from PDF.
 * Used by AiService.extractStructuredWithContext() for structured output.
 *
 * The PDF contains:
 * - Header metadata (beneficiary, total RECs retired, retiring account holder, etc.)
 * - A table with certificate details (NAR ID, project name, project type, serial numbers, quantity)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetNarRetireCertExtract {

    // Header metadata - shared across all records
    private String beneficiary;
    private String totalRecsRetired;
    private String retiringAccountHolder;
    private String retirementReasonDetails;
    private String retirementDate;

    // Table rows - one per certificate line
    private List<RetireCertRow> rows;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetireCertRow {
        private String narId;
        private String projectName;
        private String projectType;
        private String narSerialNumbers;
        private String quantity;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetireCertMetadata {
        private String beneficiary;
        private String totalRecsRetired;
        private String retiringAccountHolder;
        private String retirementReasonDetails;
        private String retirementDate;
    }
}
