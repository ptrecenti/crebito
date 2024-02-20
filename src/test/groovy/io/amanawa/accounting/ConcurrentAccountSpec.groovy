package io.amanawa.accounting

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import groovy.sql.Sql
import io.amanawa.accounting.jdbc.FeatureAccount
import io.amanawa.accounting.jdbc.FeatureCustomers
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import spock.util.concurrent.PollingConditions

import java.sql.Connection

@Testcontainers
class ConcurrentAccountSpec extends Specification {

    @Shared
    Bank bank

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
        config.transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
        config.jdbcUrl = database.jdbcUrl
        config.username = database.username
        config.password = database.password
        config.maximumPoolSize = 16
        source = new HikariDataSource(config)


        sql = new Sql(source)
        sql.execute ConcurrentAccountSpec.class.getResourceAsStream('/sql/ddl.sql').text
        sql.execute ConcurrentAccountSpec.class.getResourceAsStream('/sql/dml.sql').text
    }

    @Unroll
    def "should correctly process transactions"() {
        given:
        System.setProperty(FeatureAccount.LOCK_STRATEGY_PROP, strategy)
        System.setProperty(FeatureCustomers.CUSTOMERS_CACHE_ENABLED_PROP, cache)
        bank = new Bank(FeatureCustomers.fromSystemProperties(source))

        when:
        (1..numOfThreads).parallelStream().forEach {
            Thread.start {
                bank.process(customer, new Transaction(amount, operation as char, description))
            }
        }

        then:
        conditions.eventually {
            def total = sql.firstRow("select coalesce(sum(case when tipo = 'd' then (valor * -1) else valor end),0) as total from transacoes where cliente_id = $customer").total as long
            assert bank.summary(customer).amount() == total
            assert total != 0
            assert source.getHikariPoolMXBean().getThreadsAwaitingConnection() <= 0
        }


        where:
        customer | amount  | operation   | description | numOfThreads | strategy    | cache
        1        | 10L     | 'c' as char | 'Credit'    | 16           | 'OPTIMIST'  | 'false'
        1        | 10L     | 'c' as char | 'Credit'    | 16           | 'OPTIMIST'  | 'true'
        1        | 10L     | 'c' as char | 'Credit'    | 16           | 'PESSIMIST' | 'false'
        1        | 10L     | 'c' as char | 'Credit'    | 16           | 'PESSIMIST' | 'true'
        2        | 1000L   | 'd' as char | 'Debit'     | 16           | 'PESSIMIST' | 'false'
        3        | 100000L | 'd' as char | 'Debit'     | 16           | 'OPTIMIST'  | 'false'
        4        | 100L    | 'd' as char | 'Debit'     | 16           | 'OPTIMIST'  | 'true'
        5        | 100L    | 'd' as char | 'Debit'     | 16           | 'PESSIMIST' | 'true'

    }
}
