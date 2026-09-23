package com.library.model;

import java.util.Map;

/** A physical Computer Science book held by the library. */
public class Book extends LibraryItem {
    private String isbn;
    private String publisher;

    public Book(int id, String title, String author, String isbn, String publisher,
                String category, int yearPublished, String description, ItemStatus status) {
        super(id, title, author, category, yearPublished, description, status);
        this.isbn = isbn;
        this.publisher = publisher;
    }

    @Override
    public String getItemType() {
        return "Book";
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    /** Case-insensitive match against title, author, or ISBN, used by catalog search. */
    public boolean matches(String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String needle = query.toLowerCase();
        return getTitle().toLowerCase().contains(needle)
                || getAuthor().toLowerCase().contains(needle)
                || (isbn != null && isbn.toLowerCase().contains(needle));
    }

    @Override
    public Map<String, Object> toJson() {
        Map<String, Object> json = baseJson();
        json.put("isbn", isbn);
        json.put("publisher", publisher);
        return json;
    }
}
