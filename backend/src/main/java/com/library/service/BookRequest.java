package com.library.service;

/** Immutable input for creating or updating a Book, parsed from a request body. */
public record BookRequest(
        String title,
        String author,
        String isbn,
        String publisher,
        String category,
        int yearPublished,
        String description,
        String status
) {
}
