package com.seibel.cancer.common.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO representing an incoming document upload or ingestion event.
 * Used across REST (website/agent) and FileLoader ingestion paths.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocIncomingDto {

    /** Temporary transaction ID for tracing request through upload → loader → database */
    private String transactionId;

    /** Who submitted or owns the document (may be null for system ingestions) */
    private String customer;

    /** Origin of ingestion: "web", "ai-agent", "email", "fileloader", etc. */
    private String inputSource;

    /** Tracking system type: "ERCOT", "M-RETS", "NAR", etc. */
    private String trackingSystemType;

    /** Year for file storage path (e.g., "2025") */
    private String year;

    /** Logical document type: "CSV", "PDF", "IMAGE", etc. */
    private String documentType;

    /** Original filename (if known) */
    private String filename;

    /** MIME type (e.g., text/csv, application/pdf) */
    private String contentType;

    /** External ID or UUID for traceability (assigned at database save time) */
    private String extid;

    /** When the system first received the file */
    private LocalDateTime receivedAt;

    /** Optional arbitrary metadata (classification, tags, headers, etc.) */
    private Map<String, String> metadata;

    /** Optional free-text notes */
    private String notes;

    /** Storage location — e.g., full path or S3 URI */
    private String storagePath;

    /** Size in bytes (optional) */
    private Long sizeBytes;
}
