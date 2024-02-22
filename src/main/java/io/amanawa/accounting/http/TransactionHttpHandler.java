package io.amanawa.accounting.http;

import com.fasterxml.jackson.jr.ob.JSON;
import io.amanawa.accounting.Customers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;


public final class TransactionHttpHandler implements HttpHandler {

    private final JSON json;
    private final Customers customers;

    public TransactionHttpHandler(JSON json, Customers customers) {
        this.json = json;
        this.customers = customers;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        BankRoutes.postTransaction(exchange, json, customers);
    }

}
