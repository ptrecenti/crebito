package io.amanawa.rinha;

import com.fasterxml.jackson.jr.annotationsupport.JacksonAnnotationExtension;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSONObjectException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.amanawa.accounting.Bank;
import io.amanawa.accounting.http.ApiRoutes;
import io.amanawa.accounting.http.StatementHttpHandler;
import io.amanawa.accounting.http.TransactionHttpHandler;
import io.amanawa.accounting.jdbc.FeatureAccount;
import io.amanawa.accounting.jdbc.FeatureCustomers;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;

import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.System.Logger.Level.INFO;

public class Crebito {

    private static final System.Logger log = System.getLogger(Crebito.class.getName());

    public static void main(String[] args) {
        int scale = Integer.parseInt(System.getenv().getOrDefault("SCALE_FACTOR", "1"));
        int ioThreads = Math.max(Runtime.getRuntime().availableProcessors(), 2) * scale;
        int workerThreads = ioThreads * 8 * scale;
        int dbPool = workerThreads + 6 + Integer.parseInt(System.getenv().getOrDefault("DB_POOL_PLUS", "1"));
        final ExecutorService pool = Executors.newFixedThreadPool(workerThreads);
        final HikariConfig config = new HikariConfig();
        config.setMaximumPoolSize(workerThreads);
        config.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
        config.setUsername(System.getenv().getOrDefault("DB_USER", "rinha"));
        config.setPassword(System.getenv().getOrDefault("DB_PASS", "rinha"));
        config.addDataSourceProperty("serverName", System.getenv().getOrDefault("DB_HOSTNAME", "db"));
        config.addDataSourceProperty("portNumber", System.getenv().getOrDefault("DB_PORT", "5432"));
        config.addDataSourceProperty("databaseName", System.getenv().getOrDefault("DB_NAME", "rinha"));
        config.setMaximumPoolSize(dbPool);
        config.setMinimumIdle(Math.max(workerThreads / 2, 2));
        config.setTransactionIsolation("TRANSACTION_READ_COMMITTED");

        final HikariDataSource source = new HikariDataSource(config);

        final JSON json = JSON.builder().register(JacksonAnnotationExtension.std).build();

        final Bank bank = new Bank(FeatureCustomers.fromSystemProperties(source));

        final RoutingHandler routes = Handlers.routing()
                .post("/clientes/{id}/transacoes", new TransactionHttpHandler(pool, json, bank))
                .get("/clientes/{id}/extrato", new StatementHttpHandler(pool, json, bank))
                .setFallbackHandler(ApiRoutes::notFound)
                .setInvalidMethodHandler(ApiRoutes::notFound);

        final HttpHandler root = Handlers.exceptionHandler(routes)
                .addExceptionHandler(NoSuchElementException.class, ApiRoutes::notFound)
                .addExceptionHandler(JSONObjectException.class, ApiRoutes::unprocessed)
                .addExceptionHandler(Throwable.class, ApiRoutes::serverError);

        Undertow.builder()
                .addHttpListener(8080, "0.0.0.0")
                .setIoThreads(ioThreads)
                .setWorkerThreads(workerThreads)
                .setHandler(root)
                .build()
                .start();
        log.log(INFO, "Required database connections: {0}\nlock strategy: {1}\ncustomer cache: {2}",
                dbPool,
                System.getProperty(FeatureAccount.LOCK_STRATEGY_PROP, "NOT-SET"),
                System.getProperty(FeatureCustomers.CUSTOMERS_CACHE_ENABLED_PROP, "NOT-SET")
        );

    }
}
