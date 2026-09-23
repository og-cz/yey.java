package com.library.model;

/** Lifecycle state of a borrowing transaction. */
public enum TransactionStatus {
    ON_LOAN("On loan"),
    OVERDUE("Overdue"),
    RETURNED("Available");

    private final String display;

    TransactionStatus(String display) {
        this.display = display;
    }

    public String display() {
        return display;
    }
}
