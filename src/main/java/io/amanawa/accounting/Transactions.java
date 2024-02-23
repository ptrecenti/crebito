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
    void add(Transaction transaction);

    /**
     * List set of transactions.
     *
     * @return an iterable list of transactions.
     */
    Iterable<Transaction> iterate();

}
