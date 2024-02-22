package io.amanawa.accounting;

/**
 * List of transactions.
 */
public interface Transactions {

    /**
     * Adds a new transaction.
     *
     * @param transaction to add.
     * @return true when the transaction is added to the storage, otherwise false.
     */
    boolean add(Transaction transaction);

    /**
     * List set of transactions.
     *
     * @return an iterable list of transactions.
     */
    Iterable<Transaction> iterate();

}
