package io.amanawa.accounting

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import groovy.sql.Sql
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.concurrent.PollingConditions

@Testcontainers
class ConcurrentAccountSpec extends Specification {

    @Shared
    Customers customers

    @Shared
    PostgreSQLContainer database = new PostgreSQLContainer("postgres:latest")
            .withDatabaseName("rinha")
            .withUsername("rinha")
            .withPassword("rinha")

    @Shared
    HikariDataSource source

    @Shared
    Sql sql

    def conditions = new PollingConditions(timeout: 20, initialDelay: 5)

    def setupSpec() {
        def config = new HikariConfig()
        config.setDataSourceClassName("org.postgresql.ds.PGSimpleDataSource")
        config.addDataSourceProperty("serverName", database.host);
        config.addDataSourceProperty("portNumber", database.getMappedPort(5432));
        config.addDataSourceProperty("databaseName", database.databaseName);
        config.setUsername(database.username)
        config.setPassword(database.password)
        config.setMaximumPoolSize(90)
        config.setMinimumIdle(Math.max((int) (18 / 2), 2))
        config.setTransactionIsolation("TRANSACTION_READ_COMMITTED")

        source = new HikariDataSource(config)
        customers = new Customers(source)

        sql = new Sql(source)
        sql.execute ConcurrentAccountSpec.class.getResourceAsStream('/sql/ddl.sql').text
        sql.execute ConcurrentAccountSpec.class.getResourceAsStream('/sql/dml.sql').text
    }

    @Unroll
    def "should correctly process transactions"() {
        when:
        (1..numOfThreads).parallelStream().forEach {
            Thread.start {
                def amount = new Random().nextLong(10000L) + 1L as long
                customers.customer(customer).account().withdraw(amount as long, "Debit")
                customers.customer(customer).account().balance()
                customers.customer(customer).account().statement()

            }
        }
        (1..numOfThreads).parallelStream().forEach {
            Thread.start {
                def amount = new Random().nextLong(10000L) + 1L as long
                customers.customer(customer).account().deposit(amount as long, "Credit")
                customers.customer(customer).account().balance()
                customers.customer(customer).account().statement()
            }
        }

        then:
        conditions.eventually {
            def total = sql.firstRow("select coalesce(sum(case when tipo = 'd' then (valor * -1) else valor end),0) as total from transacoes where cliente_id = $customer").total as long
            assert customers.customer(customer).account().balance().amount() == total
            assert total != 0
            assert source.getHikariPoolMXBean().getThreadsAwaitingConnection() <= 0
        }


        where:
        customer | numOfThreads
        1        | 30
        2        | 30
        3        | 30
        4        | 30
        5        | 30

    }
}
