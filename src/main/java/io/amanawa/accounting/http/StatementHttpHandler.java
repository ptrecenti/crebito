package io.amanawa.accounting.http;

import com.fasterxml.jackson.jr.ob.JSON;
import io.amanawa.accounting.Bank;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.util.concurrent.ExecutorService;


public final class StatementHttpHandler implements HttpHandler {

    private final ExecutorService pool;
    private final JSON json;
    private final Bank bank;

    public StatementHttpHandler(ExecutorService pool, JSON json, Bank bank) {
        this.pool = pool;
        this.json = json;
        this.bank = bank;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        BankRoutes.getBalance(exchange, pool, json, bank);
    }

}
