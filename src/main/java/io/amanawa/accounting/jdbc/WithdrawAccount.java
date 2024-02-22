package io.amanawa.accounting.jdbc;

import io.amanawa.accounting.*;
import io.amanawa.jdbc.JdbcSession;
import io.amanawa.jdbc.Outcome;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

public final class WithdrawAccount implements Account {

    private final JdbcSession session;
    private final Transactions transactions;
    private final long customerId;
    private final Account readOnly;
    private final Object lock = new Object();

    public WithdrawAccount(Account readOnly, JdbcSession session, long customerId) {
        this.readOnly = readOnly;
        this.session = session;
        this.transactions = new JdbcTransactions(session, customerId);
        this.customerId = customerId;
    }

    @Override
    public boolean withdraw(long amount, CharSequence description) {
        synchronized (lock) {
            try {
                final Balance balance = balance();
                boolean processed = false;
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
                        boolean added = transactions.add(new Transaction(Optional.of(customerId), amount, 'd', description, Optional.of(Instant.now())));
                        if (added) {
                            processed = true;
                        }
                    }
                }
                if (processed) {
                    session.commit();
                } else {
                    session.rollback();
                }
                return processed;
            } catch (SQLException thrown) {
                try {
                    session.rollback();
                } catch (SQLException nothingToDo) {
                    throw new IllegalStateException("There is nothing to do here", nothingToDo);
                }
                throw new IllegalStateException("Fail to optimistic lock withdraw", thrown);
            }finally {
                session.autoCommit(true);
            }
        }
    }

    @Override
    public boolean deposit(long amount, CharSequence description) {
        throw new UnsupportedOperationException("Use the deposit account");
    }

    @Override
    public Balance balance() {
        return this.readOnly.balance();
    }


    @Override
    public Statement statement() {
        return this.readOnly.statement();
    }

}
