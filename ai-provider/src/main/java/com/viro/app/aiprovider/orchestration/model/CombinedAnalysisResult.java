package com.viro.app.aiprovider.orchestration.model;

public record CombinedAnalysisResult(
        String visionAnalysis,
        String technicalAnalysis
) {}