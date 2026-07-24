package com.viro.app.aiprovider.orchestration.model;

public record DocumentAnalysis(
        String extractedText,
        String summary,
        String keyInformation
) {}