package com.seibel.basic.testutils;

import com.seibel.basic.common.enums.ActiveEnum;
import com.seibel.basic.database.db.entity.BaseDb;
import org.apache.commons.lang3.RandomStringUtils;

import java.time.LocalDateTime;

public class DomainBuilderUtils {

    public static final int SIZE_RANDOM = 10;

    // csv
    public static final int SIZE_UNIQUE = 64;
    public static final int SIZE_LABEL= 32;
    public static final int SIZE_VERSION = 16;
    public static final int SIZE_STATUS = 32;
    // type
    public static final int SIZE_CODE = 8;
    public static final int SIZE_NAME = 32;
    public static final int SIZE_DESC = 255;

    // csv
    public static final String BASE_UNIQUE = "Unq_";
    public static final String BASE_LABEL = "Ver_";
    public static final String BASE_VERSION = "Ver_";
    public static final String BASE_STATUS = "Sta_";
    // type
    public static final String BASE_CODE = "Cod_";
    public static final String BASE_NAME = "Nam_";
    public static final String BASE_DESC = "Des_";

    // csv
    public static final int SUFFIX_MIN_UNIQUE = 4;
    public static final int SUFFIX_MIN_LABEL = 4;
    public static final int SUFFIX_MIN_VERSION = 4;
    public static final int SUFFIX_MIN_STATUS = 4;
    // type
    public static final int SUFFIX_MIN_CODE = 4;
    public static final int SUFFIX_MIN_NAME = 4;
    public static final int SUFFIX_MIN_DESC = 4;

    private static void setBaseSyncFields(BaseDb item) {
        item.setCreatedAt(LocalDateTime.now());
        item.setUpdatedAt(LocalDateTime.now());
        item.setActive(ActiveEnum.ACTIVE);
        item.setDeletedAt(null);
    }


    // ===== Unique =====
    public static String getUniqueRandom() {
        return getUniqueRandom(BASE_UNIQUE, randomString());
    }

    public static String getUniqueRandom(String label) {
        return getUniqueRandom(label, randomString());
    }

    public static String getUniqueRandom(String label, String random) {
        if (label == null) label = "";

        int maxLabelLength = SIZE_UNIQUE - SUFFIX_MIN_UNIQUE;
        if (label.length() > maxLabelLength) {
            label = label.substring(0, maxLabelLength);
        }

        return buildWithLabel(label, SIZE_UNIQUE, random);
    }

    // ===== Label =====
    public static String getLabelRandom() {
        return getLabelRandom(BASE_LABEL, randomString());
    }

    public static String getLabelRandom(String label) {
        return getLabelRandom(label, randomString());
    }

    public static String getLabelRandom(String label, String random) {
        if (label == null) label = "";

        int maxLabelLength = SIZE_LABEL - SUFFIX_MIN_LABEL;
        if (label.length() > maxLabelLength) {
            label = label.substring(0, maxLabelLength);
        }

        return buildWithLabel(label, SIZE_LABEL, random);
    }

    // ===== Version =====
    public static String getVersionRandom() {
        return getVersionRandom(BASE_VERSION, randomString());
    }

    public static String getVersionRandom(String label) {
        return getVersionRandom(label, randomString());
    }

    public static String getVersionRandom(String label, String random) {
        if (label == null) label = "";

        int maxLabelLength = SIZE_VERSION - SUFFIX_MIN_VERSION;
        if (label.length() > maxLabelLength) {
            label = label.substring(0, maxLabelLength);
        }

        return buildWithLabel(label, SIZE_VERSION, random);
    }

    // ===== Status =====
    public static String getStatusRandom() {
        return getStatusRandom(BASE_STATUS, randomString());
    }

    public static String getStatusRandom(String label) {
        return getStatusRandom(label, randomString());
    }

    public static String getStatusRandom(String label, String random) {
        if (label == null) label = "";

        int maxLabelLength = SIZE_STATUS - SUFFIX_MIN_STATUS;
        if (label.length() > maxLabelLength) {
            label = label.substring(0, maxLabelLength);
        }

        return buildWithLabel(label, SIZE_STATUS, random);
    }

    // ===== Code =====
    public static String getCodeRandom() {
        return getCodeRandom(BASE_CODE, randomString());
    }

    public static String getCodeRandom(String label) {
        return getCodeRandom(label, randomString());
    }

    public static String getCodeRandom(String label, String random) {
        if (label == null) label = "";

        int maxLabelLength = SIZE_CODE - SUFFIX_MIN_CODE;
        if (label.length() > maxLabelLength) {
            label = label.substring(0, maxLabelLength);
        }

        return buildWithLabel(label, SIZE_CODE, random);
    }

    // ===== Name =====
    public static String getNameRandom() {
        return getNameRandom(BASE_NAME, randomString());
    }

    public static String getNameRandom(String label) {
        return getNameRandom(label, randomString());
    }

    public static String getNameRandom(String label, String random) {
        if (label == null) label = "";

        int maxLabelLength = SIZE_NAME - SUFFIX_MIN_NAME;
        if (label.length() > maxLabelLength) {
            label = label.substring(0, maxLabelLength);
        }

        return buildWithLabel(label, SIZE_NAME, random);
    }

    // ===== Description =====
    public static String getDescriptionRandom() {
        return getDescriptionRandom(BASE_DESC, randomString());
    }

    public static String getDescriptionRandom(String label) {
        return getDescriptionRandom(label, randomString());
    }

    public static String getDescriptionRandom(String label, String random) {
        if (label == null) label = "";

        int maxLabelLength = SIZE_DESC - SUFFIX_MIN_DESC;
        if (label.length() > maxLabelLength) {
            label = label.substring(0, maxLabelLength);
        }

        return buildWithLabel(label, SIZE_DESC, random);
    }

    // **************************************************************************
    // **************************************************************************
    // ===== Shared builder =====
    private static final int MIN_SUFFIX = 4;
    private static final int MAX_SUFFIX = 8;

    public static String buildWithLabel(String label, int maxSize, String random) {
        if (label == null) label = "";
        if (random == null) random = "";

        int available = maxSize - label.length();
        if (available <= 0) {
            return label.substring(0, Math.min(maxSize, label.length()));
        }

        int minRandom = Math.min(MIN_SUFFIX, available);
        int maxRandom = Math.min(MAX_SUFFIX, available);

        StringBuilder suffix = new StringBuilder(
                random.length() > maxRandom ? random.substring(0, maxRandom) : random
        );

        while (suffix.length() < minRandom) {
            suffix.append(RandomStringUtils.randomAlphanumeric(1).toUpperCase());
        }

        return label + suffix;
    }

    // ===== Random string generator =====
    public static String randomString() {
        return randomString(SIZE_RANDOM);
    }

    public static String randomString(int length) {
        return RandomStringUtils.randomAlphanumeric(length).toUpperCase();
    }

    // ===== Email =====
    public static String getEmailRandom() {
        return getEmailRandom("test");
    }

    public static String getEmailRandom(String prefix) {
        String random = randomString(8).toLowerCase();
        return prefix.toLowerCase() + "_" + random + "@example.com";
    }

    // ===== Phone =====
    public static String getPhoneRandom() {
        String areaCode = RandomStringUtils.randomNumeric(3);
        String exchange = RandomStringUtils.randomNumeric(3);
        String subscriber = RandomStringUtils.randomNumeric(4);
        return "(" + areaCode + ") " + exchange + "-" + subscriber;
    }
}
