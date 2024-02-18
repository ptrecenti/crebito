package io.amanawa.accounting;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Collection;
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
     * @return true when the withdrawal is successfully processed, otherwise false.
     */
    boolean withdraw(long amount, CharSequence description);

    /**
     * Deposit to the account.
     *
     * @param amount      to deposit.
     * @param description of the deposit.
     */
    void deposit(long amount, CharSequence description);

    /**
     * Generates the account summary balance.
     *
     * @return the summary balance.
     */
    Balance summary();

    /**
     * Generates the account statement with a summary and the lasted top 10 transactions.
     *
     * @return the statement.
     */
    Statement statement();

    /**
     * Current account balance summary.
     *
     * @param amount of the balance.
     * @param limit  of the account.
     * @param when   optional date of when the was taken.
     */
    record Balance(
            long amount,
            @JsonProperty("limite")
            long limit,
            Optional<Instant> when) {

        @JsonProperty("data_extrato")
        public String data() {
            return when.map(Instant::toString).orElse(null);
        }

        @JsonProperty("total")
        public Long valor() {
            return when.isPresent() ? amount : null;
        }

        @JsonProperty("saldo")
        public Long saldo() {
            return when.isEmpty() ? amount : null;
        }

    }

    /**
     * Account statement
     *
     * @param balance      summary with account information.
     * @param transactions the top latest 10 transactions.
     */
    record Statement(
            @JsonProperty("saldo")
            Balance balance,
            @JsonProperty("ultimas_transacoes")
            Collection<Bank.Transaction> transactions) {
    }


}
