package com.library.repository;

import com.library.model.Transaction;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/** In-memory store for borrow/return transactions. */
public class InMemoryTransactionRepository implements Repository<Transaction, Integer> {
    private final Map<Integer, Transaction> transactions = new ConcurrentHashMap<>();
    private final AtomicInteger idSequence = new AtomicInteger(0);

    public int nextId() {
        return idSequence.incrementAndGet();
    }

    @Override
    public List<Transaction> findAll() {
        return transactions.values().stream()
                .sorted(Comparator.comparing(Transaction::getIssuedOn).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Transaction> findById(Integer id) {
        return Optional.ofNullable(transactions.get(id));
    }

    @Override
    public Transaction save(Transaction entity) {
        transactions.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public void deleteById(Integer id) {
        transactions.remove(id);
    }

    @Override
    public boolean existsById(Integer id) {
        return transactions.containsKey(id);
    }
}
