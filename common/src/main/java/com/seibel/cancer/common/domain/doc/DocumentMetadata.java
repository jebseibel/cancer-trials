package com.seibel.cancer.common.domain.doc;

import com.seibel.cancer.common.domain.BaseDomain;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * Domain model for DocumentMetadata
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DocumentMetadata extends BaseDomain {

    // === FILE IDENTITY ===
    private String transactionId;
    private String originalFilename;
    private String documentType;
    private String contentType;
    private Long sizeBytes;
    private String storagePath;

    // === BUSINESS CONTEXT ===
    private String customer;
    private String uploadSource;
    private String notes;

    // === PROCESSING STATE ===
    private String processingStatus;
    private LocalDateTime processedAt;
    private String processorType;

    // === FILE CATEGORY ===
    private String fileCategory;

    // === VERSIONING ===
    private Integer documentVersion;
    private String supersededBy;

    // === LIFECYCLE MANAGEMENT ===
    private Integer retentionDays;
    private Integer markedForDeletion;
    private LocalDateTime deletedAt;

    // === FLEXIBLE METADATA ===
    private String metadata;
    private String other;

    // === AUDIT TIMESTAMPS ===
    private LocalDateTime uploadedAt;
}
