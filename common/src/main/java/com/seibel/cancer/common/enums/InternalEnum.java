package com.seibel.cancer.common.enums;

/**
 * Interface for internal processing enums not displayed in the UI.
 * Used for internal state tracking and pipeline management.
 *
 * - name()        : Java constant name, used system-wide as the identifier
 * - isPreferred() : default/pre-selected value
 * - isActive()    : false = retired, excluded from processing
 */
public interface InternalEnum {
    String name();
    boolean isPreferred();
    boolean isActive();
}
