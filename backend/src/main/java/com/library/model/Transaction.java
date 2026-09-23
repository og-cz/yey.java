package com.library.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/** Records a single borrow/return cycle of a book by a member. */
public class Transaction {
    private static final double LATE_FEE_PER_DAY = 5.0;

    private final int id;
    private final Book book;
    private final Member member;
    private final LocalDate issuedOn;
    private final LocalDate dueDate;
    private LocalDate returnedOn;

    public Transaction(int id, Book book, Member member, LocalDate issuedOn, LocalDate dueDate) {
        this.id = id;
        this.book = book;
        this.member = member;
        this.issuedOn = issuedOn;
        this.dueDate = dueDate;
        this.returnedOn = null;
    }

    public int getId() {
        return id;
    }

    public Book getBook() {
        return book;
    }

    public Member getMember() {
        return member;
    }

    public LocalDate getIssuedOn() {
        return issuedOn;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalDate getReturnedOn() {
        return returnedOn;
    }

    public boolean isOpen() {
        return returnedOn == null;
    }

    public void markReturned(LocalDate returnedOn) {
        this.returnedOn = returnedOn;
    }

    /** Status is derived, never stored, so it always reflects today's date. */
    public TransactionStatus getStatus() {
        if (returnedOn != null) {
            return TransactionStatus.RETURNED;
        }
        return LocalDate.now().isAfter(dueDate) ? TransactionStatus.OVERDUE : TransactionStatus.ON_LOAN;
    }

    /** Whole-peso penalty for a still-open, overdue loan; zero otherwise. */
    public double computeOverduePenalty() {
        if (getStatus() != TransactionStatus.OVERDUE) {
            return 0.0;
        }
        long daysLate = ChronoUnit.DAYS.between(dueDate, LocalDate.now());
        return daysLate * LATE_FEE_PER_DAY;
    }

    public Map<String, Object> toJson() {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", id);
        json.put("bookId", book.getId());
        json.put("title", book.getTitle());
        json.put("author", book.getAuthor());
        json.put("member", member.getName());
        json.put("studentId", member.getStudentId());
        json.put("issuedOn", issuedOn.toString());
        json.put("dueDate", dueDate.toString());
        json.put("returnedOn", returnedOn == null ? null : returnedOn.toString());
        json.put("status", getStatus().display());
        json.put("overduePenalty", computeOverduePenalty());
        return json;
    }
}
