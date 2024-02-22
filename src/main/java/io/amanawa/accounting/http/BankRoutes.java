package io.amanawa.accounting.http;

import com.fasterxml.jackson.jr.ob.JSON;
import io.amanawa.accounting.Customer;
import io.amanawa.accounting.Customers;
import io.amanawa.accounting.Transaction;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;

import java.io.IOException;
import java.nio.channels.Channels;
import java.util.ArrayDeque;
import java.util.List;

public final class BankRoutes {

    public static void getBalance(HttpServerExchange exchange, JSON json, Customers customers) throws IOException {
        final Customer customer = customers.customer(extractCustomerId(exchange));
        if (customer.exists()) {
            exchange.setStatusCode(StatusCodes.OK);
            exchange.getResponseSender().send(json.asString(customer.account().statement()));
        } else {
            ApiRoutes.notFound(exchange);
        }
    }

    public static void postTransaction(HttpServerExchange exchange, JSON json, Customers customers) throws IOException {
        final Transaction transaction = Transaction.fromMap(json.mapFrom(Channels.newInputStream(exchange.getRequestChannel())));
        if (!transaction.valid()) {
            ApiRoutes.unprocessed(exchange);
            return;
        }
        int customerId = extractCustomerId(exchange);
        final Customer customer = customers.customer(customerId);
        if (!customer.exists()) {
            ApiRoutes.notFound(exchange);
            return;
        }
        final boolean processed;
        if (transaction.debit()) {
            processed = customer.account().withdraw(transaction.amount(), transaction.description());
        } else {
            processed = customer.account().deposit(transaction.amount(), transaction.description());
        }

        if (processed) {
            exchange.setStatusCode(StatusCodes.OK);
            exchange.getResponseSender().send(json.asString(customer.account().balance()));
        } else {
            ApiRoutes.unprocessed(exchange);
        }
    }


    private static int extractCustomerId(HttpServerExchange exchange) {
        return Integer.parseInt(exchange.
                getQueryParameters().
                getOrDefault("id", new ArrayDeque<>(List.of(Long.toString(Long.MIN_VALUE))))
                .getFirst());
    }
}
