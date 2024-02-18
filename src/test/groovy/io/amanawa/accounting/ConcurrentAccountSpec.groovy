package io.amanawa.accounting

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import groovy.sql.Sql
import io.amanawa.accounting.jdbc.JdbcCustomers
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
    HikariDataSource dataSource

    @Shared
    Sql sql

    def conditions = new PollingConditions(timeout: 20, initialDelay: 5)

    def setupSpec() {
        def config = new HikariConfig()
        config.transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
        config.jdbcUrl = database.jdbcUrl
        config.username = database.username
        config.password = database.password
        config.maximumPoolSize = 10
        dataSource = new HikariDataSource(config)


        bank = new Bank(new JdbcCustomers(dataSource))
        sql = new Sql(dataSource)
        sql.execute ConcurrentAccountSpec.class.getResourceAsStream('/sql/init.sql').text
    }

    @Unroll
    def "should correctly process transactions"() {
        def numOfThreads = 100
        when:
        (1..numOfThreads).parallelStream().forEach {
            Thread.start {
                bank.process(customer, new Bank.Transaction(amount, operation, description))
            }
        }

        then:
        conditions.eventually {
            def total = sql.firstRow("select sum(case when tipo = 'd' then (valor * -1) else valor end) as total from transacoes where cliente_id = $customer").total as long
            assert bank.summary(customer).amount() == total
        }


        where:
        customer | amount | operation   | description | processed | expectedSummary
        1        | 10L    | 'c' as char | 'Credit'    | true      | new Account.Balance(50L, 100000L, Optional.empty())
        2        | 1000L  | 'd' as char | 'Debit'     | true      | new Account.Balance(50L, 80000L, Optional.empty())

    }
}
