package io.amanawa.accounting.http;

import com.fasterxml.jackson.jr.ob.JSON;
import io.amanawa.accounting.Bank;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;


public final class TransactionHttpHandler implements HttpHandler {

    private final JSON json;
    private final Bank bank;

    public TransactionHttpHandler(JSON json, Bank bank) {
        this.json = json;
        this.bank = bank;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        BankRoutes.postTransaction(exchange, json, bank);
    }

}
