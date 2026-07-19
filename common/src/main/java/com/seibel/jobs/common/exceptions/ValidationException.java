package com.seibel.jobs.common.exceptions;

public class ValidationException extends BaseServiceException {

    private final String fieldName;

    public ValidationException(String message) {
        super(message);
        this.fieldName = null;
    }

    public ValidationException(String fieldName, String message) {
        super(String.format("Validation failed for field '%s': %s", fieldName, message));
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}