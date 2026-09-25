package com.library.service;

import com.library.exception.BookNotAvailableException;
import com.library.exception.BookNotFoundException;
import com.library.exception.DuplicateIsbnException;
import com.library.exception.TransactionAlreadyReturnedException;
import com.library.exception.TransactionNotFoundException;
import com.library.exception.ValidationException;
import com.library.model.Book;
import com.library.model.ItemStatus;
import com.library.model.Member;
import com.library.model.Transaction;
import com.library.repository.InMemoryBookRepository;
import com.library.repository.InMemoryTransactionRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Encapsulates every library rule (validation, availability, overdue
 * detection) so the HTTP handlers stay thin translators between JSON and
 * these calls.
 */
public class LibraryService {
    private static final int LOAN_PERIOD_DAYS = 14;

    /**
     * The catalog is scoped to Computer Science titles only (course
     * requirement), so category is a closed set rather than free text.
     */
    private static final Set<String> VALID_CATEGORIES = new LinkedHashSet<>(List.of(
            "Algorithms",
            "Artificial Intelligence",
            "Computer Networks",
            "Cybersecurity",
            "Data Structures",
            "Databases",
            "Human-Computer Interaction",
            "Machine Learning",
            "Operating Systems",
            "Programming Languages",
            "Software Engineering",
            "Theory of Computation",
            "Web Development"));

    private final InMemoryBookRepository bookRepository;
    private final InMemoryTransactionRepository transactionRepository;

    public LibraryService(InMemoryBookRepository bookRepository,
                           InMemoryTransactionRepository transactionRepository) {
        this.bookRepository = bookRepository;
        this.transactionRepository = transactionRepository;
    }

    /** Seeds a handful of Computer Science titles so the UI has data on first run. */
    public void seedSampleData() {
        addBook(new BookRequest("Artificial Intelligence: A Modern Approach", "Stuart Russell", "9780134610993",
                "Pearson", "Artificial Intelligence", 2020,
                "The standard textbook covering the foundations and techniques of AI.", null));
        addBook(new BookRequest("Introduction to Algorithms", "Thomas H. Cormen", "9780262046305",
                "MIT Press", "Algorithms", 2022,
                "The standard reference on algorithm design and analysis.", null));
        addBook(new BookRequest("Clean Code", "Robert C. Martin", "9780132350884",
                "Prentice Hall", "Software Engineering", 2008,
                "A handbook of agile software craftsmanship.", null));
        addBook(new BookRequest("Design Patterns", "Erich Gamma", "9780201633610",
                "Addison-Wesley", "Software Engineering", 1994,
                "Elements of reusable object-oriented software.", null));
        addBook(new BookRequest("Computer Networking: A Top-Down Approach", "James Kurose", "9780133594140",
                "Pearson", "Computer Networks", 2016,
                "Covers networking concepts from the application layer down.", null));
    }

    /** The closed set of Computer Science subject areas books may be filed under. */
    public List<String> getCategories() {
        return List.copyOf(VALID_CATEGORIES);
    }

    public List<Book> searchBooks(String query, String statusFilter) {
        return bookRepository.findAll().stream()
                .filter(book -> book.matches(query))
                .filter(book -> statusFilter == null || statusFilter.isBlank() || "all".equalsIgnoreCase(statusFilter)
                        || book.getStatus() == ItemStatus.fromDisplay(statusFilter))
                .sorted(Comparator.comparingInt(Book::getId))
                .collect(Collectors.toList());
    }

    public Book getBook(int id) {
        return bookRepository.findById(id).orElseThrow(() -> new BookNotFoundException(id));
    }

    public Book addBook(BookRequest request) {
        validate(request);
        bookRepository.findByIsbn(request.isbn()).ifPresent(existing -> {
            throw new DuplicateIsbnException(request.isbn());
        });
        int id = bookRepository.nextId();
        Book book = new Book(id, request.title(), request.author(), request.isbn(), request.publisher(),
                request.category(), request.yearPublished(), request.description(), ItemStatus.AVAILABLE);
        return bookRepository.save(book);
    }

    public Book updateBook(int id, BookRequest request) {
        validate(request);
        Book book = getBook(id);
        bookRepository.findByIsbn(request.isbn())
                .filter(existing -> existing.getId() != id)
                .ifPresent(existing -> {
                    throw new DuplicateIsbnException(request.isbn());
                });
        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setIsbn(request.isbn());
        book.setPublisher(request.publisher());
        book.setCategory(request.category());
        book.setYearPublished(request.yearPublished());
        book.setDescription(request.description());
        if (request.status() != null && !request.status().isBlank()) {
            book.setStatus(ItemStatus.fromDisplay(request.status()));
        }
        return book;
    }

    public void deleteBook(int id) {
        if (!bookRepository.existsById(id)) {
            throw new BookNotFoundException(id);
        }
        bookRepository.deleteById(id);
    }

    public Transaction issueBook(int bookId, IssueRequest request) {
        if (request.member() == null || request.member().isBlank()) {
            throw new ValidationException("Member name is required");
        }
        if (request.studentId() == null || request.studentId().isBlank()) {
            throw new ValidationException("Student ID is required");
        }
        Book book = getBook(bookId);
        if (book.getStatus() != ItemStatus.AVAILABLE) {
            throw new BookNotAvailableException(book.getTitle());
        }
        book.setStatus(ItemStatus.ON_LOAN);
        LocalDate issuedOn = LocalDate.now();
        Transaction transaction = new Transaction(
                transactionRepository.nextId(),
                book,
                new Member(request.member(), request.studentId()),
                issuedOn,
                issuedOn.plusDays(LOAN_PERIOD_DAYS));
        return transactionRepository.save(transaction);
    }

    public Transaction returnBook(int transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
        if (!transaction.isOpen()) {
            throw new TransactionAlreadyReturnedException(transactionId);
        }
        transaction.markReturned(LocalDate.now());
        transaction.getBook().setStatus(ItemStatus.AVAILABLE);
        return transaction;
    }

    public List<Transaction> getIssuedTransactions() {
        return transactionRepository.findAll().stream()
                .filter(Transaction::isOpen)
                .collect(Collectors.toList());
    }

    public List<Transaction> getReturnedTransactions() {
        return transactionRepository.findAll().stream()
                .filter(transaction -> !transaction.isOpen())
                .collect(Collectors.toList());
    }

    private void validate(BookRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new ValidationException("Title is required");
        }
        if (request.author() == null || request.author().isBlank()) {
            throw new ValidationException("Author is required");
        }
        if (request.category() == null || VALID_CATEGORIES.stream().noneMatch(c -> c.equalsIgnoreCase(request.category()))) {
            throw new ValidationException(
                    "Category must be a Computer Science subject area: " + String.join(", ", VALID_CATEGORIES));
        }
    }
}
