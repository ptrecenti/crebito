package io.amanawa.accounting;

/**
 * Process the customer's transactions and collaborate with Account and Customers storage.
 */
public final class Bank {

    private final NotEmptyCustomers customers;

    /**
     * Constructs with the customers lists.
     *
     * @param customers list.
     */
    public Bank(Customers customers) {
        this.customers = new NotEmptyCustomers(customers);
    }

    /**
     * Process customer transactions.
     * Conditions to process a transaction are:
     * <ul>
     *     <li>Customer must exists</li>
     *     <li>Transaction must be valid</li>
     *     <li>Debit transaction can not violate the customer credit limit</li>
     *
     * </ul>
     *
     * @param customerId  unique identification.
     * @param transaction positive amount transacted.
     * @return true when transaction is processed otherwise false.
     */
    public boolean process(long customerId, Transaction transaction) {
        if (!transaction.valid()) {
            return false;
        }
        final Customer customer = customers.filteredBy(customerId).firstOrThrown();
        if (transaction.debit()) {
            return customer.account().withdraw(transaction.amount(), transaction.description());
        } else {
            customer.account().deposit(transaction.amount(), transaction.description());
        }
        return true;
    }

    /**
     * Generates the account statement with a summary and the lasted top 10 transactions.
     *
     * @param customerId unique identification.
     * @return the summary.
     * @see Account#balance()
     */
    public Balance summary(long customerId) {
        return customers.filteredBy(customerId).firstOrThrown().account().balance();
    }

    /**
     * Generates the account statement with a summary and the lasted top 10 transactions.
     *
     * @param customerId unique identification.
     * @return the statement.
     * @see Account#statement()
     */
    public Statement statement(long customerId) {
        return customers.filteredBy(customerId).firstOrThrown().account().statement();
    }

}
