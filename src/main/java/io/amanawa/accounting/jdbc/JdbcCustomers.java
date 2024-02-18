package io.amanawa.accounting.jdbc;

import io.amanawa.accounting.Customer;
import io.amanawa.accounting.Customers;
import io.amanawa.cache.FixedSizeLruMap;
import io.amanawa.jdbc.JdbcSession;
import io.amanawa.jdbc.ListOutcome;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

public final class JdbcCustomers implements Customers {

    private final Map<Long, Customer> cache;
    private final DataSource source;
    private final JdbcSession session;
    private final long filter;
    private final Object lock = new Object();

    public JdbcCustomers(DataSource source) {
        this(source, Map.of(), 0L);
    }

    private JdbcCustomers(DataSource source, Map<Long, Customer> cache, long filter) {
        this.filter = filter;
        this.session = new JdbcSession(source);
        this.source = source;
        this.cache = Collections.synchronizedMap(new FixedSizeLruMap<>(100));
        this.cache.putAll(cache);
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
                        .select(new ListOutcome<>(rset -> {
                            final long id = rset.getLong(1);
                            final Customer jdbcCustomer = new JdbcCustomer(new JdbcSession(source), id);
                            if (this.cache.containsKey(id)) {
                                return this.cache.get(id);
                            } else {
                                this.cache.put(id, jdbcCustomer);
                            }
                            return this.cache.get(id);
                        }));
            } catch (SQLException thrown) {
                throw new IllegalStateException("fail to list customers.", thrown);
            }
        }
    }

    @Override
    public Customers filteredBy(long customerId) {
        return new JdbcCustomers(source, cache, customerId);
    }
}
