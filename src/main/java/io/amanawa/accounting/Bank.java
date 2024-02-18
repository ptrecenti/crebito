package io.amanawa.accounting;

import java.util.Optional;


public final class Bank {
    private final Customers customers;

    public Bank(Customers customers) {
        this.customers = customers;
    }

    /**
     * Process a transaction.
     *
     * @param transaction to process
     * @return the {@link Process} result
     */
    public Process process(Transaction transaction) {
        final Customer customer = customers.customer(transaction.customerId().orElseThrow());
        if (!customer.exists()) {
            return new Process(false, false, Optional.empty());
        }
        if (!transaction.valid()) {
            return new Process(false, true, Optional.empty());
        }
        return new Process(
                transaction.valid(),
                customer.exists(),
                transaction.debit() ?
                        customer.account().withdraw(transaction.amount(), transaction.description()) :
                        customer.account().deposit(transaction.amount(), transaction.description())
        );
    }
}
