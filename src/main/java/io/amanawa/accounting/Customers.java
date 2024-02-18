package io.amanawa.accounting;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * List of customers.
 */
public interface Customers {

    /**
     * List customers.
     *
     * @return an iterable list of customers.
     */
    Iterable<Customer> iterate();

    /**
     * Filters the list of customers by the customer id.
     *
     * @param customerId to filter the customer list.
     * @return the Customers reference with the filter to apply.
     */
    Customers filteredBy(long customerId);

    final class Sugars implements Customers {

        private final Customers source;
        private final long customerId;

        public Sugars(Customers source) {
            this(source, 0L);
        }

        public Sugars(Customers source, long customerId) {
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
        public Sugars filteredBy(long customerId) {
            return new Sugars(source.filteredBy(customerId), customerId);
        }
    }
}
