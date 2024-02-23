package io.amanawa.accounting.sql;

import io.amanawa.accounting.Account;
import io.amanawa.accounting.Balance;

import java.util.Optional;
import java.util.concurrent.*;

import static java.lang.System.Logger.Level.WARNING;


final class RetryDepositAccount implements Account {

    private static final System.Logger logger = System.getLogger(RetryDepositAccount.class.getName());
    private final ExecutorService executorService;
    private final Account source;

    RetryDepositAccount(Account source) {
        this.source = source;
        this.executorService = Executors.newCachedThreadPool();
    }

    @Override
    public Optional<Balance> deposit(long amount, CharSequence description) {
        final Optional<Balance> firstTry = this.source.deposit(amount, description);
        if (firstTry.isPresent()) {
            return firstTry;
        }
        final Future<Optional<Balance>> lastTry = executorService.submit(() -> {
            Optional<Balance> deposit = source.deposit(amount, description);
            if (deposit.isPresent()) {
                return deposit;
            }
            while (deposit.isEmpty()) {
                try {
                    deposit = source.deposit(amount, description);
                    Thread.sleep(5);
                } catch (InterruptedException thrown) {
                    logger.log(WARNING, "Fail to sleep 5 ms for waiting to insert a deposit", thrown);
                }
            }
            return deposit;
        });
        try {
            return lastTry.get(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException thrown) {
            logger.log(WARNING, "Fail to retry deposit", thrown);
            return Optional.empty();
        }
    }

}
