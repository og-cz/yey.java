package com.library.exception;

public class TransactionAlreadyReturnedException extends LibraryException {
    public TransactionAlreadyReturnedException(int id) {
        super("Transaction " + id + " was already returned");
    }

    @Override
    public int statusCode() {
        return 409;
    }
}
