package com.seibel.cancer.common.domain.enums.crsfac;

import com.seibel.cancer.common.domain.enums.DisplayableEnum;
import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Lifecycle status of a FacilityOutput record — tracks where the record is in the
 * data pipeline (e.g. Loaded, Imported, Active, Resolved).
 *
 * This is NOT the CRS attestation status. Do not confuse with CrsTrackingAttestationStatus,
 * which tracks the facility's standing with the CRS registry (e.g. Approved, New Application,
 * Approved - Pending Renewal). These are two completely separate concerns:
 *
 *   FacStatus                        — lifecycle of the FacilityOutput record itself
 *   CrsTrackingAttestationStatus     — CRS registry status of the facility
 *
 * Stored in: facility_output.status (as name(), e.g. "LOADED", "ACTIVE")
 */
@Getter
public enum FacStatus implements DisplayableEnum {
    ACTIVE("Active", 1, true, true, false),
    FETCHED("Fetched", 2, true, true, false),
    IMPORTED("Imported", 3, true, true, false),
    LOADED("Loaded", 4, true, true, false),
    MANUAL("Manual", 5, true, true, false),
    NEW("New", 6, true, true, true),
    RESOLVED("Resolved", 7, true, true, false),
    STAGED("Staged", 8, true, true, false);

    private final String displayValue;
    private final int sortOrder;
    private final boolean displayable;
    private final boolean active;
    private final boolean preferred;

    FacStatus(String displayValue, int sortOrder, boolean displayable, boolean active, boolean preferred) {
        this.displayValue = displayValue;
        this.sortOrder = sortOrder;
        this.displayable = displayable;
        this.active = active;
        this.preferred = preferred;
    }

    public static List<FacStatus> activeValues() {
        return Arrays.stream(values()).filter(v -> v.active).toList();
    }

    public static List<FacStatus> displayableValues() {
        return Arrays.stream(values())
                .filter(v -> v.active && v.displayable)
                .sorted(Comparator.comparingInt(FacStatus::getSortOrder))
                .toList();
    }

    public static FacStatus getDefault() {
        for (FacStatus status : values()) {
            if (status.preferred) return status;
        }
        throw new IllegalStateException("No default defined for FacStatus");
    }

    public static FacStatus fromString(String value) {
        if (value == null || value.isBlank()) return getDefault();
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown FacStatus value: " + value);
        }
    }

    public static boolean isValid(String value) {
        try { valueOf(value); return true; } catch (IllegalArgumentException e) { return false; }
    }

    public static FacStatus fromDisplayValue(String value) {
        if (value == null || value.isBlank()) return getDefault();
        for (FacStatus s : values()) {
            if (s.matchesDisplayValue(value)) return s;
        }
        throw new IllegalArgumentException("Unknown FacStatus displayValue: " + value);
    }
}
