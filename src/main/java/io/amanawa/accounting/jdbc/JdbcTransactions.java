package io.amanawa.accounting.jdbc;

import io.amanawa.accounting.Transaction;
import io.amanawa.accounting.Transactions;
import io.amanawa.jdbc.JdbcSession;
import io.amanawa.jdbc.ListOutcome;
import io.amanawa.jdbc.Outcome;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Optional;

import static java.lang.System.Logger.Level.DEBUG;

public final class JdbcTransactions implements Transactions {
    private static final System.Logger log = System.getLogger(JdbcTransactions.class.getName());
    private final JdbcSession session;
    private final String sortBy;
    private final String orderBy;
    private final int limit;
    private final long customerId;
    private final MessageDigest keyGenerator;
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
        this.keyGenerator = md5();
    }

    private static MessageDigest md5() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException thrown) {
            throw new IllegalStateException("Fail to create message digest", thrown);
        }
    }

    @Override
    public void add(Transaction transaction) {
        synchronized (lock) {
            try {
                session.sql("""
                                insert into transacoes (id,cliente_id,valor,tipo,descricao,realizada_em)
                                values (?,?,?,?,?,?)""")
                        .set(key(transaction))
                        .set(transaction.customerId().orElseThrow())
                        .set(transaction.amount())
                        .set(transaction.operation())
                        .set(transaction.description())
                        .set(transaction.when().orElseThrow())
                        .insert(Outcome.VOID);
                log.log(DEBUG, "transaction added {0}", transaction);
            } catch (SQLException thrown) {
                throw new IllegalStateException("Fail to insert transaction", thrown);
            }
        }
    }

    private String key(Transaction transaction) {
        return new BigInteger(1, keyGenerator.digest(transaction.toString().getBytes(StandardCharsets.UTF_8))).toString(16);
    }

    @Override
    public Iterable<Transaction> iterate() {
        synchronized (lock) {
            try {
                return session.sql("""
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
