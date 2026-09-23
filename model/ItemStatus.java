package com.library.model;

/** Availability state of a catalog item. */
public enum ItemStatus {
    AVAILABLE("Available"),
    ON_LOAN("On loan");

    private final String display;

    ItemStatus(String display) {
        this.display = display;
    }

    public String display() {
        return display;
    }

    /** Maps a frontend-facing label ("Available" / "On loan") back to the enum. */
    public static ItemStatus fromDisplay(String display) {
        for (ItemStatus status : values()) {
            if (status.display.equalsIgnoreCase(display)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown item status: " + display);
    }
}
