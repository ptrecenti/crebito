package io.amanawa.accounting;

public interface Customers {

    /**
     * Obtain a customer instance by his unique identifier.
     *
     * @param id the unique identifier
     * @return an instance of customer.
     */
    Customer customer(long id);
}
