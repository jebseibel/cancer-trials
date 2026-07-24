package com.seibel.cancer.common.domain.constants;

import java.util.Arrays;
import java.util.List;

/**
 * Constants for accepted file types in document uploads.
 * Centralizes MIME type validation across the application.
 */
public final class AcceptedFileTypes {

    private AcceptedFileTypes() {
        // Prevent instantiation
    }

    // Accepted MIME types
    public static final String PDF = "application/pdf";
    public static final String CSV = "text/csv";
    public static final String ZIP = "application/zip";
    public static final String PNG = "image/png";
    public static final String JPEG = "image/jpeg";
    public static final String EXCEL_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    public static final String EXCEL_XLS = "application/vnd.ms-excel";

    // File extensions (for display/logging)
    public static final String EXT_PDF = ".pdf";
    public static final String EXT_CSV = ".csv";
    public static final String EXT_ZIP = ".zip";
    public static final String EXT_PNG = ".png";
    public static final String EXT_JPG = ".jpg";
    public static final String EXT_JPEG = ".jpeg";
    public static final String EXT_XLSX = ".xlsx";
    public static final String EXT_XLS = ".xls";

    /**
     * Get list of all accepted MIME types
     */
    public static List<String> getAllowedMimeTypes() {
        return Arrays.asList(PDF, CSV, ZIP, PNG, JPEG, EXCEL_XLSX, EXCEL_XLS);
    }

    /**
     * Get list of all accepted file extensions (for display)
     */
    public static List<String> getAllowedExtensions() {
        return Arrays.asList(EXT_PDF, EXT_CSV, EXT_ZIP, EXT_PNG, EXT_JPG, EXT_JPEG, EXT_XLSX, EXT_XLS);
    }

    /**
     * Check if a MIME type is allowed
     */
    public static boolean isAllowed(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return getAllowedMimeTypes().stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(mimeType));
    }

    /**
     * Get human-readable description of allowed types
     */
    public static String getAllowedTypesDescription() {
        return "PDF, CSV, ZIP, PNG, JPEG, Excel";
    }
}
