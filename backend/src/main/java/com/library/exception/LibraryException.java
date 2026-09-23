package com.library.exception;

/**
 * Base for every domain error the service layer can raise. Each subclass
 * knows its own HTTP status, so the HTTP layer can report it without a
 * growing if/else chain of type checks.
 */
public abstract class LibraryException extends RuntimeException {
    protected LibraryException(String message) {
        super(message);
    }

    public abstract int statusCode();
}
