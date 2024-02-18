package io.amanawa.accounting.http;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;

public final class ApiRoutes {

    public static void notFound(HttpServerExchange exchange) {
        exchange.setStatusCode(StatusCodes.NOT_FOUND);
        exchange.getResponseSender().send(StatusCodes.NOT_FOUND_STRING);
    }

    public static void serverError(HttpServerExchange exchange) {
        exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
        exchange.getResponseSender().send("Oppsss...");
    }

    public static void unprocessed(HttpServerExchange exchange) {
        exchange.setStatusCode(StatusCodes.UNPROCESSABLE_ENTITY);
        exchange.getResponseSender().send(StatusCodes.UNPROCESSABLE_ENTITY_STRING);
    }
}
