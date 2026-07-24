package com.seibel.cancer.common.domain.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Getter
public enum TrackingSystem implements DisplayableEnum {
    ERCOT("Ercot", 1, true, true, false, true),
    MIRECS("MIRECS", 2, true, true, false, false),
    MRETS("M-RETS", 3, true, true, false, true),
    NAR("NAR", 4, true, true, false, true),
    NCRETS("NC-RETS", 5, true, true, false, false),
    NEGIS("NE-GIS", 6, true, true, false, false),
    NYGATS("NYGATS", 7, true, true, false, false),
    PJMGATS("PJM-GATS", 8, true, true, false, false),
    TIGRS("TIGRS", 9, true, true, false, false),
    WREGIS("WREGIS", 10, true, true, false, true),
    UNKNOWN("Unknown", 99, false, true, true, false);

    private final String displayValue;
    private final int sortOrder;
    private final boolean displayable;
    private final boolean active;
    private final boolean preferred;
    private final boolean displayInUploads;

    TrackingSystem(String displayValue, int sortOrder, boolean displayable, boolean active, boolean preferred, boolean displayInUploads) {
        this.displayValue = displayValue;
        this.sortOrder = sortOrder;
        this.displayable = displayable;
        this.active = active;
        this.preferred = preferred;
        this.displayInUploads = displayInUploads;
    }

    public static List<TrackingSystem> displayInUploadsValues() {
        return Arrays.stream(values()).filter(v -> v.displayInUploads).toList();
    }

    public static List<TrackingSystem> activeValues() {
        return Arrays.stream(values()).filter(v -> v.active).toList();
    }

    public static List<TrackingSystem> displayableValues() {
        return Arrays.stream(values())
                .filter(v -> v.active && v.displayable)
                .sorted(Comparator.comparingInt(TrackingSystem::getSortOrder))
                .toList();
    }

    public static TrackingSystem getDefault() {
        for (TrackingSystem s : values()) {
            if (s.preferred) return s;
        }
        throw new IllegalStateException("No default defined for TrackingSystem");
    }

    public static boolean isValid(String value) {
        try {
            valueOf(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public static TrackingSystem fromString(String value) {
        if (value == null || value.isBlank()) return getDefault();
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown TrackingSystem: " + value);
        }
    }

    public static TrackingSystem fromDisplayValue(String input) {
        if (input == null || input.isBlank()) return getDefault();
        for (TrackingSystem s : values()) {
            if (s.matchesDisplayValue(input)) return s;
        }
        throw new IllegalArgumentException("Unknown TrackingSystem display value: " + input);
    }
}
