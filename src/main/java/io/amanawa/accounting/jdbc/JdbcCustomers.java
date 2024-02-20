package io.amanawa.accounting.jdbc;

import io.amanawa.accounting.Customer;
import io.amanawa.accounting.Customers;
import io.amanawa.jdbc.JdbcSession;
import io.amanawa.jdbc.ListOutcome;

import javax.sql.DataSource;
import java.sql.SQLException;

public final class JdbcCustomers implements Customers {

    private final DataSource source;
    private final JdbcSession session;
    private final long filter;
    private final Object lock = new Object();


    public JdbcCustomers(DataSource source) {
        this(source, 0L);
    }

    private JdbcCustomers(DataSource source, long filter) {
        this.filter = filter;
        this.session = new JdbcSession(source);
        this.source = source;
    }

    @Override
    public Iterable<Customer> iterate() {
        synchronized (lock) {
            try {
                return session.sql("""
                                select id
                                from clientes
                                where id = ?
                                """)
                        .set(filter)
                        .select(new ListOutcome<>(rset -> new JdbcCustomer(source, rset.getLong(1))));
            } catch (SQLException thrown) {
                throw new IllegalStateException("fail to list customers.", thrown);
            }
        }
    }

    @Override
    public Customers filteredBy(long customerId) {
        return new JdbcCustomers(source, customerId);
    }
}
