package com.viro.app.aiprovider.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple chat response DTO - Industry Standard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private String content;
    private String provider;
    private String model;

    // Optional metadata
    private Long processingTimeMs;
    private Integer inputTokens;
    private Integer outputTokens;
}
