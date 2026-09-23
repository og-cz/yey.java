package com.library.repository;

import com.library.model.Book;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** In-memory Book store. Swappable later for a JDBC-backed implementation without touching callers. */
public class InMemoryBookRepository implements Repository<Book, Integer> {
    private final Map<Integer, Book> books = new ConcurrentHashMap<>();
    private final AtomicInteger idSequence = new AtomicInteger(0);

    public int nextId() {
        return idSequence.incrementAndGet();
    }

    @Override
    public List<Book> findAll() {
        return List.copyOf(books.values());
    }

    @Override
    public Optional<Book> findById(Integer id) {
        return Optional.ofNullable(books.get(id));
    }

    public Optional<Book> findByIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return Optional.empty();
        }
        return books.values().stream()
                .filter(book -> isbn.equalsIgnoreCase(book.getIsbn()))
                .findFirst();
    }

    @Override
    public Book save(Book entity) {
        books.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public void deleteById(Integer id) {
        books.remove(id);
    }

    @Override
    public boolean existsById(Integer id) {
        return books.containsKey(id);
    }
}
