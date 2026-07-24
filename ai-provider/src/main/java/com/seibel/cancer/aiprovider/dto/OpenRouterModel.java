package com.seibel.cancer.aiprovider.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record OpenRouterModel(
        String id,
        String name,
        String description,
        @JsonProperty("context_length") int contextLength,
        Pricing pricing,
        boolean supportsVision,
        @JsonProperty("input_modalities") List<String> inputModalities
) {
    public record Pricing(
            String prompt,
            String completion
    ) {}
}
