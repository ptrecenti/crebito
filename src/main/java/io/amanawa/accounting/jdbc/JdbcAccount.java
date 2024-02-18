package io.amanawa.accounting.jdbc;

import io.amanawa.accounting.Account;
import io.amanawa.accounting.Bank;
import io.amanawa.accounting.Transactions;
import io.amanawa.jdbc.JdbcSession;
import io.amanawa.jdbc.SingleOutcome;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static java.lang.System.Logger.Level.DEBUG;

public final class JdbcAccount implements Account {

    private static final System.Logger log = System.getLogger(JdbcAccount.class.getName());
    private final JdbcSession session;
    private final Transactions transactions;
    private final long customerId;

    private final Object lock = new Object();

    public JdbcAccount(JdbcSession session, long customerId) {
        this.session = session;
        this.customerId = customerId;
        this.transactions = new JdbcTransactions(session, customerId);
    }

    @Override
    public boolean withdraw(long amount, CharSequence description) {
        synchronized (lock) {
            try {
                final boolean[] processed = {false};
                session.autoCommit(false)
                        .sql("""
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
                    transactions.add(new Bank.Transaction(Optional.of(customerId), amount, 'd', description, Optional.of(Instant.now())));
                    session.commit();
                } else {
                    log.log(DEBUG, "NOT processed - rollback");
                    session.rollback();
                }
                return processed[0];
            } catch (SQLException thrown) {
                throw new IllegalStateException("Fail to withdraw", thrown);
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
                transactions.add(new Bank.Transaction(Optional.of(customerId), amount, 'c', description, Optional.of(Instant.now())));
                session.commit();
            } catch (SQLException thrown) {
                throw new IllegalStateException("Fail to deposit", thrown);
            }
        }
    }

    @Override
    public Balance summary() {
        return summary(null);
    }

    public Balance summary(Instant when) {
        return new Balance(
                balance(),
                limit(),
                Optional.ofNullable(when)
        );
    }

    @Override
    public Statement statement() {
        return new Statement(
                summary(Instant.now()),
                topTransactions()
        );
    }

    private Collection<Bank.Transaction> topTransactions() {
        return StreamSupport.stream(transactions.iterate().spliterator(), false).toList();
    }

    private long limit() {
        synchronized (lock) {
            try {
                return session.sql("""
                                select limite
                                from clientes
                                where id = ?
                                """)
                        .set(customerId)
                        .select(new SingleOutcome<>(Long.class));
            } catch (SQLException thrown) {
                throw new IllegalStateException("fail to list customers.", thrown);
            }
        }
    }

    private long balance() {
        synchronized (lock) {
            try {
                return session.sql("""
                                select valor
                                from saldos
                                where cliente_id = ?
                                """)
                        .set(customerId)
                        .select(new SingleOutcome<>(Long.class));
            } catch (SQLException thrown) {
                throw new IllegalStateException("fail to list customers.", thrown);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JdbcAccount that = (JdbcAccount) o;
        return customerId == that.customerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId);
    }
}
