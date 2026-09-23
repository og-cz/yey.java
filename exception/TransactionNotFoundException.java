package com.library.exception;

public class TransactionNotFoundException extends LibraryException {
    public TransactionNotFoundException(int id) {
        super("No transaction found with id " + id);
    }

    @Override
    public int statusCode() {
        return 404;
    }
}
