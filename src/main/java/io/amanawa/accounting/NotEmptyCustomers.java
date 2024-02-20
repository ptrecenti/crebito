package io.amanawa.accounting;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

public final class NotEmptyCustomers implements Customers {

    private final Customers source;
    private final long customerId;

    public NotEmptyCustomers(Customers source) {
        this(source, 0L);
    }

    public NotEmptyCustomers(Customers source, long customerId) {
        this.source = source.filteredBy(customerId);
        this.customerId = customerId;
    }

    public Optional<Customer> first() {
        final Iterator<Customer> iterator = iterate().iterator();
        return iterator.hasNext() ? Optional.of(iterator.next()) : Optional.empty();
    }

    public Customer firstOrThrown() {
        return first().orElseThrow(() -> new NoSuchElementException("Customer id:  %s not found".formatted(customerId)));
    }

    @Override
    public Iterable<Customer> iterate() {
        return source.iterate();
    }

    @Override
    public NotEmptyCustomers filteredBy(long customerId) {
        return new NotEmptyCustomers(source.filteredBy(customerId), customerId);
    }
}
