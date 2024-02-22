package io.amanawa.accounting;

import io.amanawa.accounting.jdbc.JdbcCustomer;
import io.amanawa.cache.FixedSizeLruMap;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Process the customer's transactions and collaborate with Account and Customers storage.
 */
public final class Customers {

    private final Map<Long, Customer> customers;
    private final DataSource source;
    private final Object lock = new Object();
    public Customers(DataSource source) {
        this.customers = new FixedSizeLruMap<>(100);
        this.source = source;
    }

    /**
     * Obtain a customer.
     *
     * @param customerId unique identifier
     * @return a {@link Customer}
     */
    public Customer customer(long customerId) {
        synchronized (lock) {
            if (!this.customers.containsKey(customerId)) {
                this.customers.put(customerId, new JdbcCustomer(source, customerId));
            }
            return this.customers.get(customerId);
        }
    }

}
