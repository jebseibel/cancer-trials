package com.seibel.cancer.common.domain.enums.retire;

import com.seibel.cancer.common.domain.enums.DisplayableEnum;
import lombok.Getter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Status values for retirement upload batches (aggregate status derived from detail records).
 *
 * PROCESSING - PDF extraction in progress
 * COMPLETED - PDF extraction finished, awaiting first review
 * PENDING_REVIEW - Has records with status NEW or CHANGED (awaiting promotion)
 * PROMOTED - All records have been promoted
 * REJECTED - All records have been rejected
 * FAILED - Upload processing failed (error during extraction)
 */
@Getter
public enum RetCertUploadStatus implements DisplayableEnum {
    COMPLETED("Completed", 1, true, true, false),
    FAILED("Failed", 2, true, true, false),
    PENDING_REVIEW("Pending Review", 3, true, true, false),
    PROCESSING("Processing", 4, true, true, true),
    PROMOTED("Promoted", 5, true, true, false),
    REJECTED("Rejected", 6, true, true, false);

    private final String displayValue;
    private final int sortOrder;
    private final boolean displayable;
    private final boolean active;
    private final boolean preferred;

    RetCertUploadStatus(String displayValue, int sortOrder, boolean displayable, boolean active, boolean preferred) {
        this.displayValue = displayValue;
        this.sortOrder = sortOrder;
        this.displayable = displayable;
        this.active = active;
        this.preferred = preferred;
    }

    public static List<RetCertUploadStatus> activeValues() {
        return Arrays.stream(values()).filter(v -> v.active).toList();
    }

    public static List<RetCertUploadStatus> displayableValues() {
        return Arrays.stream(values())
                .filter(v -> v.active && v.displayable)
                .sorted(Comparator.comparingInt(RetCertUploadStatus::getSortOrder))
                .toList();
    }

    public static RetCertUploadStatus getDefault() {
        for (RetCertUploadStatus status : values()) {
            if (status.preferred) return status;
        }
        throw new IllegalStateException("No default defined for RetCertUploadStatus");
    }

    public static RetCertUploadStatus fromString(String value) {
        if (value == null || value.isBlank()) return getDefault();
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown RetCertUploadStatus value: " + value);
        }
    }

    public static RetCertUploadStatus fromDisplayValue(String value) {
        if (value == null || value.isBlank()) return getDefault();
        for (RetCertUploadStatus s : values()) {
            if (s.matchesDisplayValue(value)) return s;
        }
        throw new IllegalArgumentException("Unknown RetCertUploadStatus displayValue: " + value);
    }
}
