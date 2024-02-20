package io.amanawa.accounting;

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

}
