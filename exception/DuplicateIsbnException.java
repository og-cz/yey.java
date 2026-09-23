package com.library.exception;

public class DuplicateIsbnException extends LibraryException {
    public DuplicateIsbnException(String isbn) {
        super("A book with ISBN " + isbn + " already exists");
    }

    @Override
    public int statusCode() {
        return 409;
    }
}
