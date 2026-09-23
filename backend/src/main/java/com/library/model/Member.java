package com.library.model;

/** A borrower identified by name and student number. Immutable value object. */
public final class Member {
    private final String name;
    private final String studentId;

    public Member(String name, String studentId) {
        this.name = name;
        this.studentId = studentId;
    }

    public String getName() {
        return name;
    }

    public String getStudentId() {
        return studentId;
    }
}
