package com.library.exception;

public class BookNotFoundException extends LibraryException {
    public BookNotFoundException(int id) {
        super("No book found with id " + id);
    }

    @Override
    public int statusCode() {
        return 404;
    }
}
