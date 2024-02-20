package io.amanawa.accounting.jdbc;

import io.amanawa.accounting.Customer;
import io.amanawa.accounting.Customers;

import javax.sql.DataSource;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.System.Logger.Level.DEBUG;

public final class FeatureCustomers implements Customers {

    public static final String CUSTOMERS_CACHE_ENABLED_PROP = "customers.cache.enabled";
    private static final System.Logger log = System.getLogger(FeatureCustomers.class.getName());
    private final AtomicReference<Customers> cached;
    private final AtomicReference<Customers> uncached;
    private final Object lock = new Object();

    private FeatureCustomers(Customers cached, Customers uncached) {
        this.cached = new AtomicReference<>(cached);
        this.uncached = new AtomicReference<>(uncached);
    }

    public static synchronized Customers fromSystemProperties(DataSource source) {
        Customers uncached = new JdbcCustomers(source);
        Customers cached = new CachedCustomers(uncached);
        return new FeatureCustomers(cached, uncached);
    }

    @Override
    public Iterable<Customer> iterate() {
        synchronized (lock) {
            if (cacheEnabled()) {
                log.log(DEBUG, "Running cached customers");
                return cached.get().iterate();
            }
            log.log(DEBUG, "Running uncached customers");
            return uncached.get().iterate();
        }
    }

    @Override
    public Customers filteredBy(long customerId) {
        synchronized (lock) {
            this.cached.getAndUpdate(actual -> actual.filteredBy(customerId));
            this.uncached.getAndUpdate(actual -> actual.filteredBy(customerId));
            return this;
        }
    }

    private boolean cacheEnabled() {
        return Boolean.parseBoolean(System.getProperty(CUSTOMERS_CACHE_ENABLED_PROP, "TRUE"));
    }
}
