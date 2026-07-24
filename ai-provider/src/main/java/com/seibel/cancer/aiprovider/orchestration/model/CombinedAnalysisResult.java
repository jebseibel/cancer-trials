package com.seibel.cancer.aiprovider.orchestration.model;

public record CombinedAnalysisResult(
        String visionAnalysis,
        String technicalAnalysis
) {}