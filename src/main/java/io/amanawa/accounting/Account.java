package io.amanawa.accounting;

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
     * @return true when the withdrawal is successfully processed, otherwise false.
     */
    boolean withdraw(long amount, CharSequence description);

    /**
     * Deposit to the account.
     *
     * @param amount      to deposit.
     * @param description of the deposit.
     * @return true when the deposit is successfully processed, otherwise false.
     */
    boolean deposit(long amount, CharSequence description);

    /**
     * Generates the account summary balance.
     *
     * @return the summary balance.
     */
    Balance balance();

    /**
     * Generates the account statement with a summary and the lasted top 10 transactions.
     *
     * @return the statement.
     */
    Statement statement();


}
