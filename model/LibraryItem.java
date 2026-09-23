package com.library.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Base type for anything the library catalogs. Book is the only concrete
 * subtype today, but the split keeps catalog-wide fields (title, author,
 * availability) separate from item-specific ones and lets new item types
 * (e.g. a future Magazine) be added without touching this class.
 */
public abstract class LibraryItem {
    private final int id;
    private String title;
    private String author;
    private String category;
    private int yearPublished;
    private String description;
    private ItemStatus status;

    protected LibraryItem(int id, String title, String author, String category,
                           int yearPublished, String description, ItemStatus status) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.category = category;
        this.yearPublished = yearPublished;
        this.description = description;
        this.status = status;
    }

    /** Subtype label, e.g. "Book". Demonstrates a polymorphic hook for future item types. */
    public abstract String getItemType();

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getYearPublished() {
        return yearPublished;
    }

    public void setYearPublished(int yearPublished) {
        this.yearPublished = yearPublished;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ItemStatus getStatus() {
        return status;
    }

    public void setStatus(ItemStatus status) {
        this.status = status;
    }

    /**
     * Builds the fields common to every catalog item. Subclasses call this
     * and add their own fields before serializing to JSON.
     */
    protected Map<String, Object> baseJson() {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", id);
        json.put("title", title);
        json.put("author", author);
        json.put("category", category);
        json.put("yearPublished", yearPublished);
        json.put("description", description);
        json.put("status", status.display());
        return json;
    }

    public abstract Map<String, Object> toJson();
}
