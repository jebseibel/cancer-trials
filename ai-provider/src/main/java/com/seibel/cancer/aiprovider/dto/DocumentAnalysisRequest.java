package com.seibel.cancer.aiprovider.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * Document analysis request DTO - Industry Standard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentAnalysisRequest {

    private MultipartFile file;     // Document to analyze
    private String instruction;     // What to extract/analyze
    private String provider;        // "openai", "anthropic", or null for default

    // Optional metadata
    private String documentType;    // "invoice", "certificate", etc.
    private String userId;
}
