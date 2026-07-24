package com.seibel.cancer.aiprovider.orchestration.model;

public record ConversationResult(
        String initialResponse,
        String followUpResponse
) {}