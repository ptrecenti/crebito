package io.amanawa.accounting.jdbc;

import io.amanawa.accounting.*;
import io.amanawa.jdbc.JdbcSession;
import io.amanawa.jdbc.Outcome;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

import static java.lang.System.Logger.Level.DEBUG;

public final class OptimistLockAccount implements Account {

    private static final System.Logger log = System.getLogger(OptimistLockAccount.class.getName());
    private final JdbcSession session;
    private final Account readOnlyAccount;
    private final Transactions transactions;
    private final long customerId;
    private final Object lock = new Object();

    public OptimistLockAccount(JdbcSession session, Account readOnlyAccount, Transactions transactions, long customerId) {
        this.session = session;
        this.readOnlyAccount = readOnlyAccount;
        this.transactions = transactions;
        this.customerId = customerId;
    }

    @Override
    public boolean withdraw(long amount, CharSequence description) {
        log.log(DEBUG, "Running optimistic withdraw");
        synchronized (lock) {
            try {
                final Balance balance = balance();
                if (balance.amount() + balance.limit() - amount >= 0) {
                    final int updateCount = session
                            .autoCommit(false)
                            .sql("""
                                    update saldos set
                                    valor = ?,
                                    version = ?
                                    where cliente_id = ?
                                    and version = ?""")
                            .set(balance.amount() - amount)
                            .set(balance.version().orElseThrow() + 1)
                            .set(customerId)
                            .set(balance.version().orElseThrow())
                            .update(Outcome.UPDATE_COUNT);
                    if (updateCount > 0) {
                        transactions.add(new Transaction(Optional.of(customerId), amount, 'd', description, Optional.of(Instant.now())));
                        log.log(DEBUG, "withdraw balance: {0} limit: {1} amount:{2}", balance.amount(), balance.limit(), amount);
                        session.commit();
                        return true;
                    } else {
                        session.rollback();
                    }
                }
                return false;
            } catch (SQLException thrown) {
                throw new IllegalStateException("Fail to optimistic lock deposit", thrown);
            } finally {
                session.autoCommit(true);
            }
        }
    }

    @Override
    public void deposit(long amount, CharSequence description) {
        log.log(DEBUG, "Running optimistic deposit");
        synchronized (lock) {
            try {
                final Balance balance = balance();
                final int updateCount = session
                        .autoCommit(false)
                        .sql("""
                                update saldos set
                                valor = ?,
                                version = ?
                                where cliente_id = ?
                                and version = ?""")
                        .set(balance.amount() + amount)
                        .set(balance.version().orElseThrow() + 1)
                        .set(customerId)
                        .set(balance.version().orElseThrow())
                        .update(Outcome.UPDATE_COUNT);
                if (updateCount > 0) {
                    transactions.add(new Transaction(Optional.of(customerId), amount, 'c', description, Optional.of(Instant.now())));
                    log.log(DEBUG, "deposit balance: {0} limit: {1} amount:{2}", balance.amount(), balance.limit(), amount);
                    session.commit();
                } else {
                    session.rollback();
                }
            } catch (SQLException thrown) {
                throw new IllegalStateException("Fail to optimistic lock deposit", thrown);
            } finally {
                session.autoCommit(true);
            }
        }
    }

    @Override
    public Balance balance() {
        return this.readOnlyAccount.balance();
    }

    @Override
    public Statement statement() {
        return this.readOnlyAccount.statement();
    }
}
