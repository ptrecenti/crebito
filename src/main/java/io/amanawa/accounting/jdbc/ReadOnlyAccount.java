package io.amanawa.accounting.jdbc;

import io.amanawa.accounting.*;
import io.amanawa.jdbc.JdbcSession;
import io.amanawa.jdbc.SingleOutcome;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.StreamSupport;

public final class ReadOnlyAccount implements Account {

    private final JdbcSession session;
    private final Transactions transactions;
    private final long customerId;
    private final Object lock = new Object();

    public ReadOnlyAccount(JdbcSession session, Transactions transactions, long customerId) {
        this.session = session;
        this.transactions = transactions;
        this.customerId = customerId;
    }


    @Override
    public boolean withdraw(long amount, CharSequence description) {
        throw new UnsupportedOperationException("Choose optimistic or pessimist lock strategy");
    }


    @Override
    public void deposit(long amount, CharSequence description) {
        throw new UnsupportedOperationException("Choose optimistic or pessimist lock strategy");
    }


    @Override
    public Balance balance() {
        return balance(null);
    }


    @Override
    public Statement statement() {
        return new Statement(
                balance(Instant.now()),
                topTransactions()
        );
    }

    private Collection<Transaction> topTransactions() {
        return StreamSupport.stream(transactions.iterate().spliterator(), false).toList();
    }

    private Balance balance(Instant when) {
        synchronized (lock) {
            try {
                return session.sql("""
                                select s.valor, c.limite, s.version
                                from saldos s inner join clientes c on c.id = s.cliente_id
                                where s.cliente_id = ?""")
                        .set(customerId)
                        .select(new SingleOutcome<>(rset -> new Balance(
                                rset.getLong(1),
                                rset.getLong(2),
                                Optional.ofNullable(when),
                                Optional.of(rset.getInt(3)))
                                , false));
            } catch (SQLException thrown) {
                throw new IllegalStateException("fail to list customers.", thrown);
            }
        }
    }

}
