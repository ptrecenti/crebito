package io.amanawa.accounting.jdbc;

import io.amanawa.accounting.Account;
import io.amanawa.accounting.Balance;
import io.amanawa.accounting.Statement;
import io.amanawa.accounting.Transactions;
import io.amanawa.jdbc.JdbcSession;
import io.amanawa.jdbc.SingleOutcome;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

public final class ReadOnlyAccount implements Account {

    private static final System.Logger log = System.getLogger(ReadOnlyAccount.class.getName());
    private final JdbcSession session;
    private final Transactions transactions;
    private final long customerId;
    private final Object lock = new Object();

    public ReadOnlyAccount(JdbcSession session, long customerId) {
        this.session = session;
        this.transactions = new JdbcTransactions(session, customerId);
        this.customerId = customerId;
    }

    @Override
    public boolean withdraw(long amount, CharSequence description) {
        throw new UnsupportedOperationException("Use the withdraw account");
    }

    @Override
    public boolean deposit(long amount, CharSequence description) {
        throw new UnsupportedOperationException("Use the deposit account");
    }

    @Override
    public Balance balance() {
        return balance(null);
    }


    @Override
    public Statement statement() {
        return new Statement(
                balance(Instant.now()),
                transactions.iterate()
        );
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
