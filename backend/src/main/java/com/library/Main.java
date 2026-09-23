package com.library;

import com.library.http.BooksHandler;
import com.library.http.TransactionsHandler;
import com.library.repository.InMemoryBookRepository;
import com.library.repository.InMemoryTransactionRepository;
import com.library.service.LibraryService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

/**
 * Entry point: wires the repositories, service, and HTTP handlers together
 * and starts a JDK-only HTTP server (no framework, no external dependencies)
 * exposing the Computer Science library catalog to the shared React frontend.
 */
public final class Main {
    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        InMemoryBookRepository bookRepository = new InMemoryBookRepository();
        InMemoryTransactionRepository transactionRepository = new InMemoryTransactionRepository();
        LibraryService service = new LibraryService(bookRepository, transactionRepository);
        service.seedSampleData();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/api/books", new BooksHandler(service));
        server.createContext("/api/transactions", new TransactionsHandler(service));
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("Library backend listening on http://localhost:" + PORT);
    }
}
