package io.amanawa.accounting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Process the customer's transactions and collaborate with Account and Customers storage.
 */
public final class Bank {

    private final Customers.Sugars customers;

    /**
     * Constructs with the customers lists.
     *
     * @param customers list.
     */
    public Bank(Customers customers) {
        this.customers = new Customers.Sugars(customers);
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
            return customer.account().withdraw(transaction.amount, transaction.description);
        } else {
            customer.account().deposit(transaction.amount, transaction.description);
        }
        return true;
    }

    /**
     * Generates the account statement with a summary and the lasted top 10 transactions.
     *
     * @param customerId unique identification.
     * @return the summary.
     * @see Account#summary()
     */
    public Account.Balance summary(long customerId) {
        return customers.filteredBy(customerId).firstOrThrown().account().summary();
    }

    /**
     * Generates the account statement with a summary and the lasted top 10 transactions.
     *
     * @param customerId unique identification.
     * @return the statement.
     * @see Account#statement()
     */
    public Account.Statement statement(long customerId) {
        return customers.filteredBy(customerId).firstOrThrown().account().statement();
    }

    /**
     * Bank transaction holds information about request and processed transactions.
     *
     * @param customerId  of who is requesting the transaction. It's present when the transaction is processed.
     * @param amount      of the transaction.
     * @param operation   type of the transaction.
     * @param description of the transaction.
     * @param when        the transaction was realized. It's present when the transaction is processed.
     */
    public record Transaction(
            @JsonIgnore
            Optional<Long> customerId,
            @JsonProperty("valor")
            long amount,
            @JsonProperty("tipo")
            char operation,
            @JsonProperty("descricao")
            CharSequence description,

            Optional<Instant> when) {
        public Transaction(
                @JsonProperty("valor")
                long amount,
                @JsonProperty("tipo")
                char operation,
                @JsonProperty("descricao")
                CharSequence description) {
            this(Optional.empty(), amount, operation, description, Optional.empty());
        }

        public static Transaction fromMap(Map<String, Object> map) {
            return new Transaction(
                    parseAmount(map),
                    map.getOrDefault("tipo", '\0').toString().charAt(0),
                    Optional.ofNullable(map.getOrDefault("descricao", "")).orElse("").toString()
            );
        }

        private static long parseAmount(Map<String, Object> map) {
            Object amount = map.getOrDefault("valor", 0L);
            return (amount instanceof Double) ? 0L : (amount instanceof Integer) ? (int) amount : (long) amount;
        }

        @JsonProperty("realizada_em")
        public String realizadaEm() {
            return when.map(Instant::toString).orElse(null);
        }

        /**
         * Checks if the transaction is valid.
         * Valid transaction must be:
         * <ul>
         *     <li>The amount must be greater then zero.</li>
         *     <li>The operation type must be 'c' for credit or 'd' for debit.</li>
         *     <li>The description of the transaction must be not empty and have less than 10 characters.</li>
         * </ul>
         *
         * @return true the transaction is valid, otherwise false.
         */
        public boolean valid() {
            return amount > 0 &&
                    ('c' == operation || 'd' == operation) &&
                    !description.isEmpty() &&
                    description.length() <= 10;
        }

        /**
         * Checks if the transaction operation is for debit.
         *
         * @return true if the operation is equals to 'c', otherwise false.
         */
        public boolean debit() {
            return 'd' == operation;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Transaction that = (Transaction) o;
            return amount == that.amount && operation == that.operation && Objects.equals(customerId, that.customerId) && Objects.equals(description, that.description) && Objects.equals(when, that.when);
        }

        @Override
        public int hashCode() {
            return Objects.hash(customerId, amount, operation, description, when);
        }
    }
}
