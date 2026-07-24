package com.seibel.cancer.aiprovider.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Simple chat request DTO - Industry Standard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    private String prompt;
    private String systemMessage;  // Optional context
    private String provider;       // "openai", "anthropic", or null for default

    // Optional metadata
    private String userId;
    private String sessionId;
}
