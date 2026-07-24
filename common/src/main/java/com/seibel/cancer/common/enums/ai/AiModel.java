package com.seibel.cancer.common.enums.ai;

import com.seibel.cancer.common.enums.DisplayableEnum;
import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Getter
public enum AiModel implements DisplayableEnum {

    // OpenAI
    GPT_41_MINI(AiProvider.OPENAI, "gpt-4.1-mini", "GPT-4.1 Mini", 1, true, true, true),
    GPT_41(AiProvider.OPENAI, "gpt-4.1", "GPT-4.1", 2, true, true, false),

    // Anthropic
    CLAUDE_SONNET(AiProvider.ANTHROPIC, "claude-sonnet-4-6", "Claude Sonnet 4.6", 3, true, true, true),
    CLAUDE_OPUS(AiProvider.ANTHROPIC, "claude-opus-4-6", "Claude Opus 4.6", 4, true, true, false),
    CLAUDE_HAIKU(AiProvider.ANTHROPIC, "claude-haiku-4-5-20251001", "Claude Haiku 4.5", 5, true, true, false),

    // Google Gemini
    GEMINI_25_FLASH(AiProvider.GEMINI, "gemini-2.5-flash", "Gemini 2.5 Flash", 6, true, true, true),
    GEMINI_25_PRO(AiProvider.GEMINI, "gemini-2.5-pro", "Gemini 2.5 Pro", 7, true, true, false),
    GEMINI_25_FLASH_LITE(AiProvider.GEMINI, "gemini-2.5-flash-lite", "Gemini 2.5 Flash Lite", 8, true, true, false);

    private final AiProvider provider;
    private final String modelId;
    private final String displayValue;
    private final int sortOrder;
    private final boolean displayable;
    private final boolean active;
    private final boolean preferred;

    AiModel(AiProvider provider, String modelId, String displayValue,
            int sortOrder, boolean displayable, boolean active, boolean preferred) {
        this.provider = provider;
        this.modelId = modelId;
        this.displayValue = displayValue;
        this.sortOrder = sortOrder;
        this.displayable = displayable;
        this.active = active;
        this.preferred = preferred;
    }

    @Override
    public boolean isPreferred() {
        return preferred;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public static List<AiModel> activeValues() {
        return Arrays.stream(values())
                .filter(AiModel::isActive)
                .sorted(Comparator.comparingInt(AiModel::getSortOrder))
                .toList();
    }

    public static List<AiModel> forProvider(AiProvider provider) {
        return Arrays.stream(values())
                .filter(m -> m.active && m.provider == provider)
                .sorted(Comparator.comparingInt(AiModel::getSortOrder))
                .toList();
    }

    public static AiModel getDefaultForProvider(AiProvider provider) {
        return forProvider(provider).stream()
                .filter(AiModel::isPreferred)
                .findFirst()
                .orElseGet(() -> forProvider(provider).getFirst());
    }

    public static AiModel fromModelId(String modelId) {
        if (modelId == null || modelId.isBlank()) return null;
        for (AiModel m : values()) {
            if (m.modelId.equalsIgnoreCase(modelId)) return m;
        }
        throw new IllegalArgumentException("Unknown AiModel modelId: " + modelId);
    }

    public static AiModel fromString(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown AiModel value: " + value);
        }
    }
}
