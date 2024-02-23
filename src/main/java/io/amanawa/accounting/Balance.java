package io.amanawa.accounting;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Optional;

/**
 * Current account balance summary.
 *
 * @param amount of the balance.
 * @param limit  of the account.
 * @param when   optional date of when the was taken.
 */
public record Balance(
        long amount,
        @JsonProperty("limite")
        long limit,
        Optional<Instant> when,
        Optional<Integer> version) {

    public static final Balance empty = new Balance();

    public Balance() {
        this(0, 0, Optional.empty(), Optional.empty());
    }

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
