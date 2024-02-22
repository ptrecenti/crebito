package io.amanawa.accounting;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Account statement
 *
 * @param balance      summary with account information.
 * @param transactions the top latest 10 transactions.
 */
public record Statement(
        @JsonProperty("saldo")
        Balance balance,
        @JsonProperty("ultimas_transacoes")
        Iterable<Transaction> transactions) {
}
