package com.seibel.cancer.database.exceptions;

public class DatabaseFailureException extends Exception {

    private static final long serialVersionUID = 1L;

    public DatabaseFailureException() {
        super();
    }

    public DatabaseFailureException(String message) {
        super(message);
    }

    public DatabaseFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatabaseFailureException(Throwable cause) {
        super(cause);
    }
}
