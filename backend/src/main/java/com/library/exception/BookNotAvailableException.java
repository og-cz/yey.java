package com.library.exception;

public class BookNotAvailableException extends LibraryException {
    public BookNotAvailableException(String title) {
        super("\"" + title + "\" has no available copies to issue");
    }

    @Override
    public int statusCode() {
        return 409;
    }
}
