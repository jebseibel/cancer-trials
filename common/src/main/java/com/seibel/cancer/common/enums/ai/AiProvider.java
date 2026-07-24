package com.seibel.cancer.common.enums.ai;

import com.seibel.cancer.common.enums.InternalEnum;
import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Getter
public enum AiProvider implements InternalEnum {
    OPENAI("openai", 1, true, true),
    ANTHROPIC("anthropic", 2, false, true),
    GEMINI("gemini", 3, false, true),
    OPENROUTER("openrouter", 4, false, true);

    private final String displayValue;
    private final int sortOrder;
    private final boolean preferred;
    private final boolean active;

    AiProvider(String displayValue, int sortOrder, boolean preferred, boolean active) {
        this.displayValue = displayValue;
        this.sortOrder = sortOrder;
        this.preferred = preferred;
        this.active = active;
    }

    @Override
    public boolean isPreferred() {
        return preferred;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public static List<AiProvider> activeValues() {
        return Arrays.stream(values())
                .filter(AiProvider::isActive)
                .sorted(Comparator.comparingInt(AiProvider::getSortOrder))
                .toList();
    }

    public static AiProvider getDefault() {
        return Arrays.stream(values())
                .filter(AiProvider::isPreferred)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No default defined for AiProvider"));
    }

    public static AiProvider fromString(String value) {
        if (value == null || value.isBlank()) return getDefault();
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown AiProvider value: " + value);
        }
    }
}
