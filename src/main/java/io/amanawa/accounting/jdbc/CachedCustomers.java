package io.amanawa.accounting.jdbc;

import io.amanawa.accounting.Customer;
import io.amanawa.accounting.Customers;
import io.amanawa.cache.FixedSizeLruMap;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public final class CachedCustomers implements Customers {

    private final Map<Integer, Customer> cache;
    private final AtomicReference<Customers> source;
    private final Object lock = new Object();

    public CachedCustomers(Customers source) {
        this(source, 100);
    }

    private CachedCustomers(Customers source, int size) {
        this.source = new AtomicReference<>(source);
        this.cache = Collections.synchronizedMap(new FixedSizeLruMap<>(size));
    }

    @Override
    public Iterable<Customer> iterate() {
        synchronized (lock) {
            final List<Customer> result = new LinkedList<>();
            source.get().iterate().forEach(customer -> {
                if (!cache.containsKey(customer.hashCode())) {
                    cache.put(customer.hashCode(), customer);
                }
                result.add(cache.get(customer.hashCode()));
            });
            return result;
        }
    }

    @Override
    public Customers filteredBy(long customerId) {
        synchronized (lock) {
            source.getAndUpdate(actual -> actual.filteredBy(customerId));
            return this;
        }
    }
}
