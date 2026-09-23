package com.library.service;

/** Immutable input for issuing a book to a borrower. */
public record IssueRequest(String member, String studentId) {
}
