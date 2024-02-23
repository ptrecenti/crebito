package io.amanawa.accounting;

import java.util.Optional;

/**
 * Banking account process customers operations of withdraw and deposit. Interacts with Transactions list to add new
 * transactions and list it into the statements.
 */
public interface Account {


    /**
     * Withdraw from account.
     *
     * @param amount      to withdraw.
     * @param description of the withdrawal.
     * @return the new balance when it's valid, otherwise empty.
     */
    default Optional<Balance> withdraw(long amount, CharSequence description) {
        throw new UnsupportedOperationException("Withdraw not supported");
    }

    /**
     * Deposit to the account.
     *
     * @param amount      to deposit.
     * @param description of the deposit.
     * @return the new balance when it's valid, otherwise empty.
     */
    default Optional<Balance> deposit(long amount, CharSequence description) {
        throw new UnsupportedOperationException("Deposit not supported");
    }

    /**
     * Generates the account summary balance.
     *
     * @return the summary balance.
     */
    default Balance balance() {
        throw new UnsupportedOperationException("Balance not supported");
    }

    /**
     * Generates the account statement with a summary and the lasted top 10 transactions.
     *
     * @return the statement.
     */
    default Statement statement() {
        throw new UnsupportedOperationException("Statement not supported");
    }


}
