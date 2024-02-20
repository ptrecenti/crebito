package io.amanawa.accounting.jdbc;

import io.amanawa.accounting.Account;
import io.amanawa.accounting.Balance;
import io.amanawa.accounting.Statement;
import io.amanawa.accounting.Transactions;
import io.amanawa.jdbc.JdbcSession;

import javax.sql.DataSource;
import java.util.Map;

public final class FeatureAccount implements Account {

    public static final String LOCK_STRATEGY_PROP = "lock.strategy";
    private final Map<LockStrategy, Account> accounts;
    private final LockStrategy lockStrategy;


    private FeatureAccount(Map<LockStrategy, Account> accounts, LockStrategy strategy) {
        this.accounts = accounts;
        this.lockStrategy = strategy;
    }

    public static synchronized Account fromSystemProperties(DataSource source, long customerId) {
        return build(source, customerId);
    }

    private static LockStrategy lockStrategy() {
        return LockStrategy.valueOf(
                System.getProperty(LOCK_STRATEGY_PROP, "OPTIMIST")
        );
    }

    private static FeatureAccount build(DataSource source, long customerId) {
        final JdbcSession writingSession = new JdbcSession(source);
        final JdbcSession readingSession = new JdbcSession(source);
        final Transactions readingTransactions = new JdbcTransactions(readingSession, customerId);
        final Account readOnlyAccount = new ReadOnlyAccount(readingSession, readingTransactions, customerId);
        final Transactions transactions = new JdbcTransactions(writingSession, customerId);
        final Account optimist = new OptimistLockAccount(writingSession, readOnlyAccount, transactions, customerId);
        final Account pessimist = new PessimistLockAccount(writingSession, readOnlyAccount, transactions, customerId);
        return new FeatureAccount(Map.of(LockStrategy.OPTIMIST, optimist, LockStrategy.PESSIMIST, pessimist), lockStrategy());
    }

    @Override
    public boolean withdraw(long amount, CharSequence description) {
        return account().withdraw(amount, description);
    }

    @Override
    public void deposit(long amount, CharSequence description) {
        account().deposit(amount, description);
    }

    @Override
    public Balance balance() {
        return account().balance();
    }

    @Override
    public Statement statement() {
        return account().statement();
    }

    private Account account() {
        return accounts.get(lockStrategy);
    }

    private enum LockStrategy {
        OPTIMIST,
        PESSIMIST
    }
}
