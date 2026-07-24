package com.seibel.cancer.common.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for AI extraction of MRETS Transaction Confirmation from PDF.
 * Used by AiService.extractStructuredWithContext() for structured output.
 *
 * The PDF contains:
 * - Header metadata (date, organization, retiring RECs count)
 * - A table with certificate details (account, id, project, fuel type, etc.)
 * - Footer metadata (transaction notes, retirement reason, eligibilities)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetMretsTransConfirmExtract {

    // Header metadata - shared across all records
    private String transactionDate;
    private String mretsOrganization;
    private String retiringRecsCount;

    // Footer metadata - shared across all records
    private String transactionNotes;
    private String retirementReason;
    private String retirementReasonDetails;
    private String toRetirementAccount;

    // Table rows - one per certificate line
    private List<TransConfirmRow> rows;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransConfirmRow {
        private String account;
        private String rowId;
        private String project;
        private String fuelType;
        private String vintage;
        private String location;
        private String quantity;
        private String serialNumber;
    }
}
