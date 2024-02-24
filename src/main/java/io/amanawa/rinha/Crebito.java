package io.amanawa.rinha;

import com.fasterxml.jackson.jr.annotationsupport.JacksonAnnotationExtension;
import com.fasterxml.jackson.jr.ob.JSON;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.amanawa.accounting.Bank;
import io.amanawa.accounting.Customers;
import io.amanawa.accounting.http.StatementHttpHandler;
import io.amanawa.accounting.http.TransactionHttpHandler;
import io.amanawa.accounting.sql.SqlCustomers;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.server.RoutingHandler;

import static java.lang.System.Logger.Level.INFO;

public class Crebito {

    private static final System.Logger log = System.getLogger(Crebito.class.getName());

    public static void main(String[] args) {
        int scale = Integer.parseInt(System.getenv().getOrDefault("SCALE_FACTOR", "30"));
        int ioThreads = Math.max(Runtime.getRuntime().availableProcessors(), 2) * scale;
        int workerThreads = ioThreads * 8;
        int dbPool = Integer.parseInt(System.getenv().getOrDefault("DB_POOL", "" + Math.max(ioThreads / 3, 2)));
        int minIdle = Math.max(dbPool / 6, 1);
        final HikariConfig config = new HikariConfig();
        config.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource");
        config.setUsername(System.getenv().getOrDefault("DB_USER", "rinha"));
        config.setPassword(System.getenv().getOrDefault("DB_PASS", "rinha"));
        config.addDataSourceProperty("serverName", System.getenv().getOrDefault("DB_HOSTNAME", "db"));
        config.addDataSourceProperty("portNumber", System.getenv().getOrDefault("DB_PORT", "5432"));
        config.addDataSourceProperty("databaseName", System.getenv().getOrDefault("DB_NAME", "rinha"));
        config.setMaximumPoolSize(dbPool);
        config.setMinimumIdle(minIdle);
        config.setTransactionIsolation("TRANSACTION_READ_COMMITTED");

        final HikariDataSource source = new HikariDataSource(config);

        final JSON json = JSON.builder().register(JacksonAnnotationExtension.std).build();

        final Customers customers = new SqlCustomers(source);
        final Bank bank = new Bank(customers);
        final RoutingHandler routes = Handlers.routing()
                .post("/clientes/{id}/transacoes", new TransactionHttpHandler(json, bank))
                .get("/clientes/{id}/extrato", new StatementHttpHandler(json, customers));

        Undertow.builder()
                .addHttpListener(8080, "0.0.0.0")
                .setIoThreads(ioThreads)
                .setWorkerThreads(workerThreads)
                .setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, true)
                .setHandler(routes)
                .build()
                .start();
        log.log(INFO, "Required database connections:{0} connections-min-idle:{1} workers:{2} io:{3}", dbPool, minIdle, workerThreads, ioThreads);

    }
}
