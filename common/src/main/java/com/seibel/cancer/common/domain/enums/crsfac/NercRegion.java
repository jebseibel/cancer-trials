package com.seibel.cancer.common.domain.enums.crsfac;

import com.seibel.cancer.common.domain.enums.DisplayableEnum;
import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * NERC region values for facilities.
 *
 * ASCC       - Alaska Systems Coordinating Council
 * FRCC       - Florida Reliability Coordinating Council
 * GUAM       - Guam
 * MRO        - Midwest Reliability Organization
 * NPCC       - Northeast Power Coordinating Council
 * PUERTO_RICO - Puerto Rico
 * RFC        - ReliabilityFirst Corporation
 * SERC       - SERC Reliability Corporation
 * TRE        - Texas Reliability Entity
 * WECC       - Western Electricity Coordinating Council
 * TBD        - To Be Determined
 * MISTAKE    - Mistake / unrecognized value
 */
@Getter
public enum NercRegion implements DisplayableEnum {
    ASCC("ASCC", 1, true, true, false),
    FRCC("FRCC", 2, true, true, false),
    GUAM("Guam", 3, true, true, false),
    MISTAKE("Mistake", 4, false, true, true),
    MRO("MRO", 5, true, true, false),
    NPCC("NPCC", 6, true, true, false),
    PUERTO_RICO("Puerto Rico", 7, true, true, false),
    RFC("RFC", 8, true, true, false),
    SERC("SERC", 9, true, true, false),
    TBD("TBD", 10, false, true, false),
    TRE("TRE", 11, true, true, false),
    WECC("WECC", 12, true, true, false);

    private final String displayValue;
    private final int sortOrder;
    private final boolean displayable;
    private final boolean active;
    private final boolean preferred;

    NercRegion(String displayValue, int sortOrder, boolean displayable, boolean active, boolean preferred) {
        this.displayValue = displayValue;
        this.sortOrder = sortOrder;
        this.displayable = displayable;
        this.active = active;
        this.preferred = preferred;
    }

    public static List<NercRegion> activeValues() {
        return Arrays.stream(values()).filter(v -> v.active).toList();
    }

    public static List<NercRegion> displayableValues() {
        return Arrays.stream(values())
                .filter(v -> v.active && v.displayable)
                .sorted(Comparator.comparingInt(NercRegion::getSortOrder))
                .toList();
    }

    public static NercRegion getDefault() {
        for (NercRegion region : values()) {
            if (region.preferred) return region;
        }
        throw new IllegalStateException("No default defined for NercRegion");
    }

    public static NercRegion fromString(String value) {
        if (value == null || value.isBlank()) return getDefault();
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown NercRegion value: " + value);
        }
    }

    public static boolean isValid(String value) {
        try { valueOf(value); return true; } catch (IllegalArgumentException e) { return false; }
    }

    public static NercRegion fromDisplayValue(String value) {
        if (value == null || value.isBlank()) return getDefault();
        for (NercRegion s : values()) {
            if (s.matchesDisplayValue(value)) return s;
        }
        throw new IllegalArgumentException("Unknown NercRegion displayValue: " + value);
    }
}
