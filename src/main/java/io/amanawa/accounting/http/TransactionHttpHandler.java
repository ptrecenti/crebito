package io.amanawa.accounting.http;

import com.fasterxml.jackson.jr.ob.JSON;
import io.amanawa.accounting.sql.SqlCustomers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;


public final class TransactionHttpHandler implements HttpHandler {

    private final JSON json;
    private final SqlCustomers sqlCustomers;

    public TransactionHttpHandler(JSON json, SqlCustomers sqlCustomers) {
        this.json = json;
        this.sqlCustomers = sqlCustomers;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        BankRoutes.postTransaction(exchange, json, sqlCustomers);
    }

}
