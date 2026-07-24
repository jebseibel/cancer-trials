package com.seibel.cancer.aiprovider.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Document analysis response DTO - Industry Standard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentAnalysisResponse {

    private String extractedText;
    private String summary;
    private Map<String, Object> extractedFields;  // Structured data

    private String provider;
    private String model;
    private Long processingTimeMs;
}
