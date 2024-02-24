package io.amanawa.accounting;

import java.util.Optional;

/**
 * Post transaction response.
 *
 * @param valid   true when the transaction payload is valid, otherwise false.
 * @param exists  true when the customer exists, otherwise false.
 * @param balance is optional balance processed.
 */
public record Process(boolean valid, boolean exists, Optional<Balance> balance) {
}
