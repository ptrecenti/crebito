package io.amanawa.accounting.sql;

import io.amanawa.accounting.*;
import io.amanawa.jdbc.JdbcSession;
import io.amanawa.jdbc.Outcome;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

final class SqlWithdrawAccount implements Account {

    private final JdbcSession session;
    private final Transactions transactions;
    private final long customerId;
    private final Account readOnly;
    private final Object lock = new Object();

    SqlWithdrawAccount(Account readOnly, Transactions transactions, JdbcSession session, long customerId) {
        this.readOnly = readOnly;
        this.session = session;
        this.transactions = transactions;
        this.customerId = customerId;
    }

    @Override
    public boolean withdraw(long amount, CharSequence description) {
        synchronized (lock) {
            try {
                final Balance balance = balance();
                if (balance.amount() + balance.limit() - amount >= 0) {
                    final boolean processed = session
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
                            .update(Outcome.UPDATE_COUNT) > 0;
                    if (processed) {
                        transactions.add(new Transaction(Optional.of(customerId), amount, 'd', description, Optional.of(Instant.now())));
                    }
                    return processed;
                }
                return false;
            } catch (SQLException thrown) {
                throw new IllegalStateException("Fail to optimistic lock withdraw", thrown);
            }
        }
    }

    @Override
    public boolean deposit(long amount, CharSequence description) {
        throw new UnsupportedOperationException("Use the deposit account");
    }

    @Override
    public Balance balance() {
        synchronized (lock) {
            return this.readOnly.balance();
        }
    }


    @Override
    public Statement statement() {
        synchronized (lock) {
            return this.readOnly.statement();
        }
    }

}
