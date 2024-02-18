package io.amanawa.accounting;

/**
 * List of transactions.
 */
public interface Transactions {

    /**
     * Adds a new transaction.
     *
     * @param transaction to add.
     */
    void add(Bank.Transaction transaction);

    /**
     * List set of transactions.
     *
     * @return an iterable list of transactions.
     */
    Iterable<Bank.Transaction> iterate();

    /**
     * Filters the transactions by the customer id.
     *
     * @param customerId to filter the transaction list
     * @return the reference for the transactions filtering by the customer id.
     */
    Transactions filteredBy(long customerId);

    /**
     * Choose the fields to sort the transactions list.
     *
     * @param field to sort the transactions list.
     * @param order to sort the transactions list it can be asc or desc.
     * @return the reference for the transactions sorting by the fields.
     */
    Transactions sortedBy(String field, String order);

    /**
     * Limits the list of transactions.
     *
     * @param limit to apply to the filter.
     * @return the reference for the transactions limited by the limit.
     */
    Transactions limitedTo(int limit);
}
