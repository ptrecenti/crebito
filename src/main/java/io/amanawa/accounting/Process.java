package io.amanawa.accounting;

import java.util.Optional;

public record Process(boolean valid, boolean exists, Optional<Balance> balance) {
}
