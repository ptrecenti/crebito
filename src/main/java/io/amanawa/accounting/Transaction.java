package io.amanawa.accounting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Bank transaction holds information about request and valid transactions.
 *
 * @param customerId  of who is requesting the transaction. It's present when the transaction is valid.
 * @param amount      of the transaction.
 * @param operation   type of the transaction.
 * @param description of the transaction.
 * @param when        the transaction was realized. It's present when the transaction is valid.
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
            long customerId,
            @JsonProperty("valor")
            long amount,
            @JsonProperty("tipo")
            char operation,
            @JsonProperty("descricao")
            CharSequence description) {
        this(Optional.of(customerId), amount, operation, description, Optional.empty());
    }

    public static Transaction fromMap(long customerId, Map<String, Object> map) {
        return new Transaction(
                customerId,
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
}
