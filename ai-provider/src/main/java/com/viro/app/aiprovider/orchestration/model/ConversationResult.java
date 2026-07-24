package com.viro.app.aiprovider.orchestration.model;

public record ConversationResult(
        String initialResponse,
        String followUpResponse
) {}