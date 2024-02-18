package io.amanawa.accounting.sql;

import io.amanawa.accounting.Account;
import io.amanawa.accounting.Balance;
import io.amanawa.accounting.Statement;
import io.amanawa.accounting.Transactions;
import io.amanawa.jdbc.JdbcSession;
import io.amanawa.jdbc.SingleOutcome;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

final class SqlReadOnlyAccount implements Account {

    private static final System.Logger logger = System.getLogger(SqlReadOnlyAccount.class.getName());
    private final JdbcSession session;
    private final Transactions transactions;
    private final long customerId;
    private final Object lock = new Object();

    SqlReadOnlyAccount(JdbcSession session, Transactions transactions, long customerId) {
        this.session = session;
        this.transactions = transactions;
        this.customerId = customerId;
    }

    @Override
    public Balance balance() {
        synchronized (lock) {
            return balance(null);
        }
    }


    @Override
    public Statement statement() {
        synchronized (lock) {
            return new Statement(
                    balance(Instant.now()),
                    transactions.iterate()
            );
        }
    }

    private Balance balance(Instant when) {
        synchronized (lock) {
            try {
                return session.sql("""
                                select s.valor, c.limite, s.versao
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
                logger.log(System.Logger.Level.WARNING, "Fail to get balance.", thrown);
                return Balance.empty;
            }
        }
    }
}
