package com.seibel.cancer.common.enums;

/**
 * Interface for all domain enums that are displayable in the UI and API.
 * Enforces consistent behavior across all domain enums.
 *
 * - name()            : Java constant name, used system-wide as the identifier (Rule 3)
 * - getDisplayValue() : human-facing label, FE rendering only (Rule 1)
 * - getSortOrder()    : ascending sort order for UI display (Rule 4)
 * - isDisplayable()   : visible in UI dropdowns (Rule 5)
 * - isActive()        : false = retired, excluded from all enum lists (Rule 9)
 * - isPreferred()     : pre-selected in UI dropdowns (Rule 6)
 */
public interface DisplayableEnum {
    String name();
    String getDisplayValue();
    int getSortOrder();
    boolean isDisplayable();
    boolean isActive();
    boolean isPreferred();

    /**
     * Normalizes a string for loose matching: lowercase, all whitespace removed.
     * Used by fromDisplayValue() in each enum to match values coming from load tables,
     * which may differ in case or spacing from the canonical displayValue.
     */
    static String normalize(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("\\s+", "");
    }

    /**
     * Returns true if the given input matches this constant's displayValue after normalization.
     */
    default boolean matchesDisplayValue(String input) {
        return DisplayableEnum.normalize(getDisplayValue()).equals(DisplayableEnum.normalize(input));
    }
}
