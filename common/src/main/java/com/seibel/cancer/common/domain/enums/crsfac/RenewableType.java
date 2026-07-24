package com.seibel.cancer.common.domain.enums.crsfac;

import com.seibel.cancer.common.domain.enums.DisplayableEnum;
import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Renewable resource type values for facility and CRS records.
 *
 * SOLAR             - Solar
 * WIND              - Wind
 * HYDRO             - Hydro
 * GEOTHERMAL        - Geothermal
 * NON_GASEOUS_BIOMASS - Non-gaseous Biomass
 * GASEOUS_BIOMASS   - Gaseous Biomass
 * MISTAKE           - Mistake (default/fallback)
 */
@Getter
public enum RenewableType implements DisplayableEnum {
    GASEOUS_BIOMASS("Gaseous Biomass", 1, true, true, false),
    GEOTHERMAL("Geothermal", 2, true, true, false),
    HYDRO("Hydro", 3, true, true, false),
    MISTAKE("Mistake", 4, false, true, true),
    NON_GASEOUS_BIOMASS("Non-gaseous Biomass", 5, true, true, false),
    SOLAR("Solar", 6, true, true, false),
    WIND("Wind", 7, true, true, false);

    private final String displayValue;
    private final int sortOrder;
    private final boolean displayable;
    private final boolean active;
    private final boolean preferred;

    RenewableType(String displayValue, int sortOrder, boolean displayable, boolean active, boolean preferred) {
        this.displayValue = displayValue;
        this.sortOrder = sortOrder;
        this.displayable = displayable;
        this.active = active;
        this.preferred = preferred;
    }

    public static List<RenewableType> activeValues() {
        return Arrays.stream(values()).filter(v -> v.active).toList();
    }

    public static List<RenewableType> displayableValues() {
        return Arrays.stream(values())
                .filter(v -> v.active && v.displayable)
                .sorted(Comparator.comparingInt(RenewableType::getSortOrder))
                .toList();
    }

    public static RenewableType getDefault() {
        for (RenewableType type : values()) {
            if (type.preferred) return type;
        }
        throw new IllegalStateException("No default defined for RenewableType");
    }

    public static RenewableType fromString(String value) {
        if (value == null || value.isBlank()) return getDefault();
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown RenewableType value: " + value);
        }
    }

    public static boolean isValid(String value) {
        try { valueOf(value); return true; } catch (IllegalArgumentException e) { return false; }
    }

    public static RenewableType fromDisplayValue(String value) {
        if (value == null || value.isBlank()) return getDefault();
        for (RenewableType s : values()) {
            if (s.matchesDisplayValue(value)) return s;
        }
        throw new IllegalArgumentException("Unknown RenewableType displayValue: " + value);
    }
}
