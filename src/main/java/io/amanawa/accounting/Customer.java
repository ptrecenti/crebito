package io.amanawa.accounting;

/**
 * Holds a reference to his account. Each customer has only one account.
 */
public interface Customer {
    Account account();
}
