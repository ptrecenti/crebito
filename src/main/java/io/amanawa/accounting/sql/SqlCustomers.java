package io.amanawa.accounting.sql;

import io.amanawa.accounting.Customer;
import io.amanawa.accounting.Customers;
import io.amanawa.jdbc.JdbcSession;

import javax.sql.DataSource;

/**
 * Process the customer's transactions and collaborate with Account and Customers storage.
 */
public final class SqlCustomers implements Customers {

    private final DataSource source;

    public SqlCustomers(DataSource source) {
        this.source = source;
    }

    @Override
    public Customer customer(long customerId) {
        final JdbcSession session = new JdbcSession(source);
        return new SqlCustomer(session, new SqlTransactions(session, customerId), customerId);
    }

}
