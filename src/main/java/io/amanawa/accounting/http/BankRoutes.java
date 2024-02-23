package io.amanawa.accounting.http;

import com.fasterxml.jackson.jr.ob.JSON;
import io.amanawa.accounting.Process;
import io.amanawa.accounting.*;
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

    public static void postTransaction(HttpServerExchange exchange, JSON json, Bank bank) throws IOException {
        final Transaction transaction = Transaction.fromMap(extractCustomerId(exchange),
                json.mapFrom(Channels.newInputStream(exchange.getRequestChannel())));

        final Process process = bank.process(transaction);

        if (process.exists() && process.valid() && process.balance().isPresent()) {
            exchange.setStatusCode(StatusCodes.OK);
            exchange.getResponseSender().send(json.asString(process.balance().get()));
            return;
        }

        if (!process.exists()) {
            ApiRoutes.notFound(exchange);
            return;
        }

        ApiRoutes.unprocessed(exchange);
    }


    private static int extractCustomerId(HttpServerExchange exchange) {
        return Integer.parseInt(exchange.
                getQueryParameters().
                getOrDefault("id", new ArrayDeque<>(List.of(Long.toString(Long.MIN_VALUE))))
                .getFirst());
    }
}
