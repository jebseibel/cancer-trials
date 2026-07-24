package com.seibel.cancer.common.domain.enums.retire;

import com.seibel.cancer.common.domain.enums.DisplayableEnum;
import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Status values for individual retirement certificate records.
 *
 * Loaded - Record has been imported from PDF but not yet reviewed/promoted
 * Promoted - Record has been reviewed and approved for use
 * Testing - Record has been inserted only to be used in testing.
 * Changed - Record has been manually edited after import
 */
@Getter
public enum RetCertRecordStatus implements DisplayableEnum {
    CHANGED("Changed", 1, true, true, false),
    LOADED("Loaded", 2, true, true, true),
    TESTING("Testing", 3, true, true, false),
    PROMOTED("Promoted", 4, true, true, false);

    private final String displayValue;
    private final int sortOrder;
    private final boolean displayable;
    private final boolean active;
    private final boolean preferred;

    RetCertRecordStatus(String displayValue, int sortOrder, boolean displayable, boolean active, boolean preferred) {
        this.displayValue = displayValue;
        this.sortOrder = sortOrder;
        this.displayable = displayable;
        this.active = active;
        this.preferred = preferred;
    }

    public static List<RetCertRecordStatus> activeValues() {
        return Arrays.stream(values()).filter(v -> v.active).toList();
    }

    public static List<RetCertRecordStatus> displayableValues() {
        return Arrays.stream(values())
                .filter(v -> v.active && v.displayable)
                .sorted(Comparator.comparingInt(RetCertRecordStatus::getSortOrder))
                .toList();
    }

    public static RetCertRecordStatus getDefault() {
        for (RetCertRecordStatus status : values()) {
            if (status.preferred) return status;
        }
        throw new IllegalStateException("No default defined for RetCertRecordStatus");
    }

    public static RetCertRecordStatus fromString(String value) {
        if (value == null || value.isBlank()) return getDefault();
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown RetCertRecordStatus value: " + value);
        }
    }

    public static RetCertRecordStatus fromDisplayValue(String value) {
        if (value == null || value.isBlank()) return getDefault();
        for (RetCertRecordStatus s : values()) {
            if (s.matchesDisplayValue(value)) return s;
        }
        throw new IllegalArgumentException("Unknown RetCertRecordStatus displayValue: " + value);
    }
}
