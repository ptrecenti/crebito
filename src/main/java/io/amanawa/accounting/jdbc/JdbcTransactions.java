package io.amanawa.accounting.jdbc;

import io.amanawa.accounting.Transaction;
import io.amanawa.accounting.Transactions;
import io.amanawa.jdbc.JdbcSession;
import io.amanawa.jdbc.ListOutcome;
import io.amanawa.jdbc.Outcome;

import java.sql.SQLException;
import java.util.Optional;

import static java.lang.System.Logger.Level.ERROR;

public final class JdbcTransactions implements Transactions {
    private static final System.Logger log = System.getLogger(JdbcTransactions.class.getName());
    private final JdbcSession session;
    private final String sortBy;
    private final String orderBy;
    private final int limit;
    private final long customerId;
    private final Object lock = new Object();


    JdbcTransactions(JdbcSession session, long customerId) {
        this(session, "realizada_em", "desc", 10, customerId);
    }

    private JdbcTransactions(JdbcSession session, String sortBy, String orderBy, int limit, long customerId) {
        this.session = session;
        this.sortBy = sortBy;
        this.orderBy = orderBy;
        this.limit = limit;
        this.customerId = customerId;
    }

    @Override
    public boolean add(Transaction transaction) {
        synchronized (lock) {
            try {
                int updates = session
                        .sql("""
                                insert into transacoes (cliente_id,valor,tipo,descricao)
                                values (?,?,?,?)""")
                        .set(transaction.customerId().orElseThrow())
                        .set(transaction.amount())
                        .set(transaction.operation())
                        .set(transaction.description())
                        .update(Outcome.UPDATE_COUNT);
                return updates > 0;
            } catch (SQLException thrown) {
                log.log(ERROR, "Fail to add a transactions", thrown);
                return false;
            }
        }
    }

    @Override
    public Iterable<Transaction> iterate() {
        synchronized (lock) {
            try {
                return session
                        .sql("""
                                select cliente_id,valor,tipo,descricao,realizada_em
                                from transacoes
                                where cliente_id = ?
                                order by %s %s
                                limit %s
                                """.formatted(sortBy, orderBy, limit))
                        .set(customerId)
                        .select(new ListOutcome<>(rset -> new Transaction(
                                Optional.of(rset.getLong(1)),
                                rset.getLong(2),
                                rset.getString(3).charAt(0),
                                rset.getString(4),
                                Optional.of(rset.getTimestamp(5).toInstant())
                        )));
            } catch (SQLException thrown) {
                throw new IllegalStateException("Fail to iterate on transactions", thrown);
            }
        }
    }
}
