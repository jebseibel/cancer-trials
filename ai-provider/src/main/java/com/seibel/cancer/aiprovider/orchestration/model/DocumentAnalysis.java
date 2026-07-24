package com.seibel.cancer.aiprovider.orchestration.model;

public record DocumentAnalysis(
        String extractedText,
        String summary,
        String keyInformation
) {}