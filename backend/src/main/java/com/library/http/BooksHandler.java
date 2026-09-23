package com.library.http;

import com.library.exception.LibraryException;
import com.library.model.Book;
import com.library.model.Transaction;
import com.library.service.BookRequest;
import com.library.service.IssueRequest;
import com.library.service.LibraryService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Routes every request under /api/books. HttpServer only matches by prefix,
 * so this handler does its own light routing on the remaining path segments.
 */
public class BooksHandler implements HttpHandler {
    private static final String PREFIX = "/api/books";

    private final LibraryService service;

    public BooksHandler(LibraryService service) {
        this.service = service;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpUtil.applyCors(exchange);
        if (HttpUtil.handlePreflight(exchange)) {
            return;
        }
        String method = exchange.getRequestMethod();
        String[] segments = HttpUtil.pathSegmentsAfter(exchange, PREFIX);
        try {
            if (segments.length == 0) {
                if ("GET".equalsIgnoreCase(method)) {
                    listBooks(exchange);
                } else if ("POST".equalsIgnoreCase(method)) {
                    createBook(exchange);
                } else {
                    HttpUtil.sendError(exchange, 405, "Method not allowed");
                }
                return;
            }
            int id = Integer.parseInt(segments[0]);
            if (segments.length == 1) {
                if ("PUT".equalsIgnoreCase(method)) {
                    updateBook(exchange, id);
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    deleteBook(exchange, id);
                } else {
                    HttpUtil.sendError(exchange, 405, "Method not allowed");
                }
                return;
            }
            if (segments.length == 2 && "issue".equals(segments[1]) && "POST".equalsIgnoreCase(method)) {
                issueBook(exchange, id);
                return;
            }
            HttpUtil.sendError(exchange, 404, "Unknown route");
        } catch (NumberFormatException e) {
            HttpUtil.sendError(exchange, 400, "Invalid book id");
        } catch (LibraryException e) {
            HttpUtil.sendError(exchange, e.statusCode(), e.getMessage());
        } catch (IllegalArgumentException e) {
            HttpUtil.sendError(exchange, 400, e.getMessage());
        }
    }

    private void listBooks(HttpExchange exchange) throws IOException {
        Map<String, String> params = HttpUtil.queryParams(exchange);
        List<Book> books = service.searchBooks(params.get("search"), params.get("status"));
        HttpUtil.sendJson(exchange, 200, books.stream().map(Book::toJson).collect(Collectors.toList()));
    }

    private void createBook(HttpExchange exchange) throws IOException {
        BookRequest request = readBookRequest(exchange);
        Book book = service.addBook(request);
        HttpUtil.sendJson(exchange, 201, book.toJson());
    }

    private void updateBook(HttpExchange exchange, int id) throws IOException {
        BookRequest request = readBookRequest(exchange);
        Book book = service.updateBook(id, request);
        HttpUtil.sendJson(exchange, 200, book.toJson());
    }

    private void deleteBook(HttpExchange exchange, int id) throws IOException {
        service.deleteBook(id);
        HttpUtil.sendNoContent(exchange);
    }

    private void issueBook(HttpExchange exchange, int id) throws IOException {
        Map<String, Object> body = JsonUtil.parseObject(HttpUtil.readBody(exchange));
        IssueRequest request = new IssueRequest(
                HttpUtil.stringField(body, "member"),
                HttpUtil.stringField(body, "studentId"));
        Transaction transaction = service.issueBook(id, request);
        HttpUtil.sendJson(exchange, 201, transaction.toJson());
    }

    private BookRequest readBookRequest(HttpExchange exchange) throws IOException {
        Map<String, Object> body = JsonUtil.parseObject(HttpUtil.readBody(exchange));
        return new BookRequest(
                HttpUtil.stringField(body, "title"),
                HttpUtil.stringField(body, "author"),
                HttpUtil.stringField(body, "isbn"),
                HttpUtil.stringField(body, "publisher"),
                HttpUtil.stringField(body, "category"),
                HttpUtil.intField(body, "yearPublished", 0),
                HttpUtil.stringField(body, "description"),
                HttpUtil.stringField(body, "status"));
    }
}
