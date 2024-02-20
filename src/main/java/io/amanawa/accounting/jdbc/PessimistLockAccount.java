package io.amanawa.accounting.jdbc;

import io.amanawa.accounting.*;
import io.amanawa.jdbc.JdbcSession;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

import static java.lang.System.Logger.Level.DEBUG;

public final class PessimistLockAccount implements Account {
    private static final System.Logger log = System.getLogger(PessimistLockAccount.class.getName());
    private final JdbcSession session;
    private final Account readOnlyAccount;
    private final Transactions transactions;
    private final long customerId;
    private final Object lock = new Object();

    public PessimistLockAccount(JdbcSession session, Account readOnlyAccount, Transactions transactions, long customerId) {
        this.session = session;
        this.readOnlyAccount = readOnlyAccount;
        this.transactions = transactions;
        this.customerId = customerId;
    }

    @Override
    public boolean withdraw(long amount, CharSequence description) {
        log.log(DEBUG, "Running pessimistic withdraw");
        synchronized (lock) {
            try {
                final boolean[] processed = {false};
                session.autoCommit(false);
                session.sql("""
                                select s.id, s.valor, c.limite
                                from saldos s inner join clientes c on c.id = s.cliente_id
                                where s.cliente_id = ?
                                for update""")
                        .set(customerId)
                        .updateConcurrent((rset, stmt) -> {
                            rset.next();
                            long balance = rset.getLong(2);
                            long limit = rset.getLong(3);
                            if (balance + limit - amount >= 0) {
                                rset.updateLong(2, balance - amount);
                                rset.updateRow();
                                log.log(DEBUG, "withdraw balance: {0} limit: {1} amount:{2}", balance, limit, amount);
                                processed[0] = true;
                            }
                            return null;
                        });
                if (processed[0]) {
                    log.log(DEBUG, "processed - commit");
                    transactions.add(new Transaction(Optional.of(customerId), amount, 'd', description, Optional.of(Instant.now())));
                    session.commit();
                } else {
                    log.log(DEBUG, "NOT processed - rollback");
                    session.rollback();
                }
                return processed[0];
            } catch (SQLException thrown) {
                throw new IllegalStateException("Fail to pessimistic lock deposit", thrown);
            } finally {
                session.autoCommit(true);
            }
        }
    }

    @Override
    public void deposit(long amount, CharSequence description) {
        synchronized (lock) {
            try {
                session.autoCommit(false)
                        .sql("""
                                select id, valor
                                from saldos
                                where cliente_id = ?
                                for update""")
                        .set(customerId)
                        .updateConcurrent((rset, stmt) -> {
                            rset.next();
                            long balance = rset.getLong(2);
                            rset.updateLong(2, balance + amount);
                            rset.updateRow();
                            log.log(DEBUG, "balance updated from {0} to {1}", balance, (balance + amount));
                            return null;
                        });
                transactions.add(new Transaction(Optional.of(customerId), amount, 'c', description, Optional.of(Instant.now())));
                session.commit();
            } catch (SQLException thrown) {
                throw new IllegalStateException("Fail to deposit", thrown);
            } finally {
                session.autoCommit(true);
            }
        }
    }

    @Override
    public Balance balance() {
        return readOnlyAccount.balance();
    }

    @Override
    public Statement statement() {
        return readOnlyAccount.statement();
    }
}
