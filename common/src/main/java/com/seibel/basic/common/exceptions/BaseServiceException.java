package com.seibel.basic.common.exceptions;

public abstract class BaseServiceException extends RuntimeException {

    public BaseServiceException(String message) {
        super(message);
    }

    public BaseServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}