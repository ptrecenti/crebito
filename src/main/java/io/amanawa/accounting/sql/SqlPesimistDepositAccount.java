package io.amanawa.accounting.sql;

import io.amanawa.accounting.Account;
import io.amanawa.accounting.Balance;
import io.amanawa.accounting.Transaction;
import io.amanawa.accounting.Transactions;
import io.amanawa.jdbc.JdbcSession;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

import static java.lang.System.Logger.Level.WARNING;

final class SqlPesimistDepositAccount implements Account {

    private static final System.Logger logger = System.getLogger(SqlPesimistDepositAccount.class.getName());
    private final Transactions transactions;
    private final JdbcSession session;
    private final long customerId;
    private final Object lock = new Object();

    SqlPesimistDepositAccount(Transactions transactions, JdbcSession session, long customerId) {
        this.session = session;
        this.transactions = transactions;
        this.customerId = customerId;
    }

    @Override
    public Optional<Balance> deposit(long amount, CharSequence description) {
        synchronized (lock) {
            try {
                Optional<Balance> updatedBalance = session
                        .sql("""
                                select s.id, s.valor, c.limite, s.versao
                                from saldos s inner join clientes c on c.id = s.cliente_id
                                where s.cliente_id = ?
                                for update""")
                        .set(customerId)
                        .updateConcurrent((rset, stmt) -> {
                            rset.next();
                            long balance = rset.getLong(2);
                            long limit = rset.getLong(3);
                            int version = rset.getInt(4);
                            long newBalance = balance + amount;
                            int newVersion = version + 1;
                            rset.updateLong(2, newBalance);
                            rset.updateInt(4, newVersion);
                            rset.updateRow();

                            return Optional.of(new Balance(
                                    newBalance,
                                    limit,
                                    Optional.empty(),
                                    Optional.of(newVersion)));
                        });
                transactions.add(new Transaction(Optional.of(customerId), amount, 'c', description, Optional.of(Instant.now())));

                return updatedBalance;
            } catch (SQLException thrown) {
                logger.log(WARNING, "Fail to optimistic lock deposit", thrown);
                return Optional.empty();
            }
        }
    }

}
