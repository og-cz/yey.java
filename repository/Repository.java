package com.library.repository;

import java.util.List;
import java.util.Optional;

/** Generic storage contract so the service layer never depends on how data is persisted. */
public interface Repository<T, ID> {
    List<T> findAll();

    Optional<T> findById(ID id);

    T save(T entity);

    void deleteById(ID id);

    boolean existsById(ID id);
}
