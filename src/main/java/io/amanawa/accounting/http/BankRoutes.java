package io.amanawa.accounting.http;

import com.fasterxml.jackson.jr.ob.JSON;
import io.amanawa.accounting.Bank;
import io.amanawa.accounting.Transaction;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;

import java.io.IOException;
import java.nio.channels.Channels;
import java.util.ArrayDeque;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;

public final class BankRoutes {

    public static void getBalance(HttpServerExchange exchange, ExecutorService pool, JSON json, Bank bank) {
        exchange.dispatch(pool, next -> {
            try {
                next.setStatusCode(StatusCodes.OK);
                next.getResponseSender().send(json.asString(bank.statement(extractCustomerId(next))));
            } catch (NoSuchElementException thrown) {
                ApiRoutes.notFound(exchange);
            }
        });
    }

    public static void postTransaction(HttpServerExchange exchange, ExecutorService pool, JSON json, Bank bank) throws IOException {
        final Transaction transaction = Transaction.fromMap(json.mapFrom(Channels.newInputStream(exchange.getRequestChannel())));
        if (!transaction.valid()) {
            ApiRoutes.unprocessed(exchange);
            return;
        }
        int customerId = extractCustomerId(exchange);
        exchange.dispatch(pool, next -> {
            try {
                if (bank.process(customerId, transaction)) {
                    exchange.setStatusCode(StatusCodes.OK);
                    next.getResponseSender().send(json.asString(bank.summary(customerId)));
                } else {
                    ApiRoutes.unprocessed(exchange);
                }
            } catch (NoSuchElementException thrown) {
                ApiRoutes.notFound(next);
            }
        });
    }


    private static int extractCustomerId(HttpServerExchange exchange) {
        return Integer.parseInt(exchange.
                getQueryParameters().
                getOrDefault("id", new ArrayDeque<>(List.of(Long.toString(Long.MIN_VALUE))))
                .getFirst());
    }
}
