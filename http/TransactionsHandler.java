package com.library.http;

import com.library.exception.LibraryException;
import com.library.model.Transaction;
import com.library.service.LibraryService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/** Routes every request under /api/transactions (issued list, returned list, mark-as-returned). */
public class TransactionsHandler implements HttpHandler {
    private static final String PREFIX = "/api/transactions";

    private final LibraryService service;

    public TransactionsHandler(LibraryService service) {
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
            if (segments.length == 1 && "issued".equals(segments[0]) && "GET".equalsIgnoreCase(method)) {
                sendTransactions(exchange, service.getIssuedTransactions());
                return;
            }
            if (segments.length == 1 && "returned".equals(segments[0]) && "GET".equalsIgnoreCase(method)) {
                sendTransactions(exchange, service.getReturnedTransactions());
                return;
            }
            if (segments.length == 2 && "return".equals(segments[1]) && "POST".equalsIgnoreCase(method)) {
                int id = Integer.parseInt(segments[0]);
                Transaction transaction = service.returnBook(id);
                HttpUtil.sendJson(exchange, 200, transaction.toJson());
                return;
            }
            HttpUtil.sendError(exchange, 404, "Unknown route");
        } catch (NumberFormatException e) {
            HttpUtil.sendError(exchange, 400, "Invalid transaction id");
        } catch (LibraryException e) {
            HttpUtil.sendError(exchange, e.statusCode(), e.getMessage());
        }
    }

    private void sendTransactions(HttpExchange exchange, List<Transaction> transactions) throws IOException {
        HttpUtil.sendJson(exchange, 200, transactions.stream().map(Transaction::toJson).collect(Collectors.toList()));
    }
}
