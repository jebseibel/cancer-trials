package com.seibel.cancer.common.enums.ai;

import com.seibel.cancer.common.enums.DisplayableEnum;
import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Getter
public enum AiLifecycle implements DisplayableEnum {
    CREATED("Created", 1, true, true, false),
    UPDATED("Updated", 2, true, true, false),
    IN_PRODUCTION("In Prod", 3, true, true, false),
    RETIRED("Retired", 4, true, true, false),
    DISCREDITED("Discredited", 5, true, true, true);

    private final String displayValue;
    private final int sortOrder;
    private final boolean displayable;
    private final boolean active;
    private final boolean preferred;

    AiLifecycle(String displayValue, int sortOrder, boolean displayable, boolean active, boolean preferred) {
        this.displayValue = displayValue;
        this.sortOrder = sortOrder;
        this.displayable = displayable;
        this.active = active;
        this.preferred = preferred;
    }

    public static List<AiLifecycle> activeValues() {
        return Arrays.stream(values()).filter(v -> v.active).toList();
    }

    public static List<AiLifecycle> displayableValues() {
        return Arrays.stream(values())
                .filter(v -> v.active && v.displayable)
                .sorted(Comparator.comparingInt(AiLifecycle::getSortOrder))
                .toList();
    }

    public static AiLifecycle getDefault() {
        for (AiLifecycle status : values()) {
            if (status.preferred) return status;
        }
        throw new IllegalStateException("No default defined for RetCertRecordEligibilityStatus");
    }

    public static AiLifecycle fromString(String value) {
        if (value == null || value.isBlank()) return getDefault();
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown RetCertRecordEligibilityStatus value: " + value);
        }
    }

    public static AiLifecycle fromDisplayValue(String value) {
        if (value == null || value.isBlank()) return getDefault();
        for (AiLifecycle s : values()) {
            if (s.matchesDisplayValue(value)) return s;
        }
        throw new IllegalArgumentException("Unknown RetCertRecordEligibilityStatus displayValue: " + value);
    }
}
