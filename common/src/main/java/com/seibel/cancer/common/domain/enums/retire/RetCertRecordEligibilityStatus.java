package com.seibel.cancer.common.domain.enums.retire;

import com.seibel.cancer.common.domain.enums.DisplayableEnum;
import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Getter
public enum RetCertRecordEligibilityStatus implements DisplayableEnum {
    DISALLOWED("Disallowed", 1, true, true, false, false),
    ELIGIBLE("Eligible", 2, true, true, false, true),
    ELIGIBLE_WITH_EXCEPTION("Eligible With Exception", 3, true, true, false, true),
    INELIGIBLE("Ineligible", 4, true, true, false, false),
    NEW("New", 5, true, true, true, false);

    private final String displayValue;
    private final int sortOrder;
    private final boolean displayable;
    private final boolean active;
    private final boolean preferred;
    private final boolean promotable;

    RetCertRecordEligibilityStatus(String displayValue, int sortOrder, boolean displayable, boolean active, boolean preferred, boolean promotable) {
        this.displayValue = displayValue;
        this.sortOrder = sortOrder;
        this.displayable = displayable;
        this.active = active;
        this.preferred = preferred;
        this.promotable = promotable;
    }

    public static List<RetCertRecordEligibilityStatus> activeValues() {
        return Arrays.stream(values()).filter(v -> v.active).toList();
    }

    public static List<RetCertRecordEligibilityStatus> displayableValues() {
        return Arrays.stream(values())
                .filter(v -> v.active && v.displayable)
                .sorted(Comparator.comparingInt(RetCertRecordEligibilityStatus::getSortOrder))
                .toList();
    }

    public static RetCertRecordEligibilityStatus getDefault() {
        for (RetCertRecordEligibilityStatus status : values()) {
            if (status.preferred) return status;
        }
        throw new IllegalStateException("No default defined for RetCertRecordEligibilityStatus");
    }

    public static RetCertRecordEligibilityStatus fromString(String value) {
        if (value == null || value.isBlank()) return getDefault();
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown RetCertRecordEligibilityStatus value: " + value);
        }
    }

    public static RetCertRecordEligibilityStatus fromDisplayValue(String value) {
        if (value == null || value.isBlank()) return getDefault();
        for (RetCertRecordEligibilityStatus s : values()) {
            if (s.matchesDisplayValue(value)) return s;
        }
        throw new IllegalArgumentException("Unknown RetCertRecordEligibilityStatus displayValue: " + value);
    }
}
