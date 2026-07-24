package com.seibel.cancer.common.domain.enums.crsfac;

import com.seibel.cancer.common.domain.enums.DisplayableEnum;
import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Status values for CRS tracking attestation.
 */
@Getter
public enum CrsTrackingAttestationStatus implements DisplayableEnum {
    APPROVED("Approved", 1, true, true, false),
    APPROVED_PENDING_RENEWAL("Approved - Pending Renewal", 2, true, true, false),
    APPROVED_VERIFICATION("Approved - Requires Additional Green-e® Verification", 3, true, true, false),
    NEW_APPLICATION("New Application", 4, true, true, true),
    REMOVED_BY_CRS("Removed by CRS", 5, true, true, false),
    REMOVED_BY_CRS_ELIGIBLE("Removed by CRS - Eligible", 6, true, true, false),
    REMOVED_BY_CRS_EXPIRATION("Removed by CRS - Expiration", 7, true, true, false),
    REMOVED_BY_CRS_NEW_DATE("Removed by CRS - New Date", 8, true, true, false),
    PENDING_CHANGE("Pending Change", 9, true, true, false);

    private final String displayValue;
    private final int sortOrder;
    private final boolean displayable;
    private final boolean active;
    private final boolean preferred;

    CrsTrackingAttestationStatus(String displayValue, int sortOrder, boolean displayable, boolean active, boolean preferred) {
        this.displayValue = displayValue;
        this.sortOrder = sortOrder;
        this.displayable = displayable;
        this.active = active;
        this.preferred = preferred;
    }

    public static List<CrsTrackingAttestationStatus> activeValues() {
        return Arrays.stream(values()).filter(v -> v.active).toList();
    }

    public static List<CrsTrackingAttestationStatus> displayableValues() {
        return Arrays.stream(values())
                .filter(v -> v.active && v.displayable)
                .sorted(Comparator.comparingInt(CrsTrackingAttestationStatus::getSortOrder))
                .toList();
    }

    public static CrsTrackingAttestationStatus getDefault() {
        for (CrsTrackingAttestationStatus status : values()) {
            if (status.preferred) return status;
        }
        throw new IllegalStateException("No default defined for CrsTrackingAttestationStatus");
    }

    public static CrsTrackingAttestationStatus fromString(String value) {
        if (value == null || value.isBlank()) return getDefault();
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown CrsTrackingAttestationStatus value: " + value);
        }
    }

    public static CrsTrackingAttestationStatus fromDisplayValue(String value) {
        if (value == null || value.isBlank()) return getDefault();
        for (CrsTrackingAttestationStatus s : values()) {
            if (s.matchesDisplayValue(value)) return s;
        }
        throw new IllegalArgumentException("Unknown CrsTrackingAttestationStatus displayValue: " + value);
    }
}
