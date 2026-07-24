package com.seibel.cancer.common.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Domain object representing a stored document.
 * Physical file content is managed by the docstorage module using Spring Content.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class StoredDocument extends BaseDomain {

    /**
     * Original filename
     */
    private String filename;

    /**
     * Content ID - path to the file in storage
     */
    private String contentId;

    /**
     * File size in bytes
     */
    private Long contentLength;

    /**
     * MIME type of the file
     */
    private String mimeType;

    /**
     * Type of entity this document is associated with (e.g., "doc_incoming", "doc_outgoing", "user")
     */
    private String entityType;

    /**
     * ID of the associated entity
     */
    private Long entityId;
}
