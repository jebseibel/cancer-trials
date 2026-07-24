package com.seibel.cancer.common.domain.enums.crsfac;

import com.seibel.cancer.common.domain.enums.DisplayableEnum;
import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Status values for CRS change records.
 *
 * LOADED - Record loaded from CRS import
 * PENDING_REVIEW - Awaiting human review
 * ERROR - Blocked due to conflicting data (e.g., effective date mismatch)
 * RULE_ACCEPTED - Auto-accepted by a reconciliation rule
 * BULK_ACCEPTED - Accepted via bulk approval action
 * RESOLVED - Record resolved/deleted
 */
@Getter
public enum CrsStatus implements DisplayableEnum {
    BULK_ACCEPTED("Bulk Accepted", 1, true, true, false),
    ERROR("Error", 2, true, true, false),
    LOADED("Loaded", 3, true, true, true),
    PENDING_REVIEW("Pending Review", 4, true, true, false),
    RESOLVED("Resolved", 5, true, true, false),
    RULE_ACCEPTED("Rule Accepted", 6, true, true, false);

    private final String displayValue;
    private final int sortOrder;
    private final boolean displayable;
    private final boolean active;
    private final boolean preferred;

    CrsStatus(String displayValue, int sortOrder, boolean displayable, boolean active, boolean preferred) {
        this.displayValue = displayValue;
        this.sortOrder = sortOrder;
        this.displayable = displayable;
        this.active = active;
        this.preferred = preferred;
    }

    public static List<CrsStatus> activeValues() {
        return Arrays.stream(values()).filter(v -> v.active).toList();
    }

    public static List<CrsStatus> displayableValues() {
        return Arrays.stream(values())
                .filter(v -> v.active && v.displayable)
                .sorted(Comparator.comparingInt(CrsStatus::getSortOrder))
                .toList();
    }

    public static CrsStatus getDefault() {
        for (CrsStatus status : values()) {
            if (status.preferred) return status;
        }
        throw new IllegalStateException("No default defined for CrsStatus");
    }

    public static CrsStatus fromString(String value) {
        if (value == null || value.isBlank()) return getDefault();
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown CrsStatus value: " + value);
        }
    }

    public static boolean isValid(String value) {
        try { valueOf(value); return true; } catch (IllegalArgumentException e) { return false; }
    }

    public static CrsStatus fromDisplayValue(String value) {
        if (value == null || value.isBlank()) return getDefault();
        for (CrsStatus s : values()) {
            if (s.matchesDisplayValue(value)) return s;
        }
        throw new IllegalArgumentException("Unknown CrsStatus displayValue: " + value);
    }
}
