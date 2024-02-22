package io.amanawa.accounting;

/**
 * Holds a reference to his account. Each customer has only one account.
 */
public interface Customer {
    /**
     * Obtain the {@link Customer} {@link Account}
     *
     * @return the {@link Customer}'s {@link Account}
     */
    Account account();

    /**
     * Verifies the existence of the customer.
     *
     * @return true when customer exists, otherwise false.
     */
    boolean exists();
}
