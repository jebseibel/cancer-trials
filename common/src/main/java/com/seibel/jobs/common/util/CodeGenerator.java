package com.seibel.jobs.common.util;

import java.util.function.Predicate;

/**
 * Utility class for auto-generating unique codes.
 * Format: NAME-CAT-SUB (6-4-4 characters) = 16 chars total with dashes
 * For entities without category/subcategory, uses name only (up to 16 chars)
 */
public class CodeGenerator {

    private static final int MAX_CODE_LENGTH = 16;
    private static final int NAME_LENGTH = 6;
    private static final int CATEGORY_LENGTH = 4;
    private static final int SUBCATEGORY_LENGTH = 4;

    /**
     * Generates a unique code from name, category, and subcategory.
     * Format: NAME-CAT-SUB (e.g., "CHEDD-CHEE-HARD")
     *
     * @param name The name to generate code from
     * @param category The category
     * @param subcategory The subcategory
     * @param codeExistsPredicate Function to check if a code already exists
     * @return A unique 16-character code in NAME-CAT-SUB format
     */
    public static String generateCode(String name, String category, String subcategory,
                                     Predicate<String> codeExistsPredicate) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be null or empty");
        }
        if (subcategory == null || subcategory.trim().isEmpty()) {
            throw new IllegalArgumentException("Subcategory cannot be null or empty");
        }

        // Extract and clean each component
        String namePart = cleanAndTruncate(extractFirstWord(name), NAME_LENGTH);
        String catPart = cleanAndTruncate(category, CATEGORY_LENGTH);
        String subPart = cleanAndTruncate(subcategory, SUBCATEGORY_LENGTH);

        // Build base code: NAME-CAT-SUB
        String baseCode = namePart + "-" + catPart + "-" + subPart;

        // Check if code exists, if not return it
        if (!codeExistsPredicate.test(baseCode)) {
            return baseCode;
        }

        // Handle collision by appending numbers (replaces last chars of name)
        for (int i = 1; i <= 99; i++) {
            String suffix = String.valueOf(i);
            int adjustedNameLength = NAME_LENGTH - suffix.length();
            String adjustedName = namePart.substring(0, Math.min(namePart.length(), adjustedNameLength)) + suffix;
            String candidateCode = adjustedName + "-" + catPart + "-" + subPart;

            if (!codeExistsPredicate.test(candidateCode)) {
                return candidateCode;
            }
        }

        throw new IllegalStateException("Unable to generate unique code for: " + name);
    }

    /**
     * Generates a unique code from name only (for entities without category/subcategory).
     * Uses up to 16 characters from the name.
     *
     * @param name The name to generate code from
     * @param codeExistsPredicate Function to check if a code already exists
     * @return A unique code up to 16 characters
     */
    public static String generateCode(String name, Predicate<String> codeExistsPredicate) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }

        // Extract first word and clean it
        String baseCode = cleanAndTruncate(extractFirstWord(name), MAX_CODE_LENGTH);

        // Check if code exists, if not return it
        if (!codeExistsPredicate.test(baseCode)) {
            return baseCode;
        }

        // Handle collision by appending numbers
        for (int i = 1; i <= 99; i++) {
            String suffix = String.valueOf(i);
            int baseLength = MAX_CODE_LENGTH - suffix.length();
            String candidateCode = baseCode.substring(0, Math.min(baseCode.length(), baseLength)) + suffix;

            if (!codeExistsPredicate.test(candidateCode)) {
                return candidateCode;
            }
        }

        throw new IllegalStateException("Unable to generate unique code for name: " + name);
    }

    /**
     * Extracts the first word from a string.
     * Words are separated by spaces.
     */
    private static String extractFirstWord(String text) {
        String trimmed = text.trim();
        int spaceIndex = trimmed.indexOf(' ');
        return spaceIndex > 0 ? trimmed.substring(0, spaceIndex) : trimmed;
    }

    /**
     * Cleans text (removes non-alphanumeric, converts to uppercase) and truncates to max length.
     */
    private static String cleanAndTruncate(String text, int maxLength) {
        String cleaned = text.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();

        if (cleaned.isEmpty()) {
            throw new IllegalArgumentException("Text must contain alphanumeric characters: " + text);
        }

        return cleaned.substring(0, Math.min(cleaned.length(), maxLength));
    }
}
