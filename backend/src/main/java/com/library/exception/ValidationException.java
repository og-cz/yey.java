package com.library.exception;

public class ValidationException extends LibraryException {
    public ValidationException(String message) {
        super(message);
    }

    @Override
    public int statusCode() {
        return 400;
    }
}
