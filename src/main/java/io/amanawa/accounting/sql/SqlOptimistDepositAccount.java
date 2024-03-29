package io.amanawa.accounting.sql;

import io.amanawa.accounting.Account;
import io.amanawa.accounting.Balance;
import io.amanawa.accounting.Transaction;
import io.amanawa.accounting.Transactions;
import io.amanawa.jdbc.JdbcSession;
import io.amanawa.jdbc.Outcome;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

final class SqlOptimistDepositAccount implements Account {

    private static final System.Logger logger = System.getLogger(SqlOptimistDepositAccount.class.getName());
    private final JdbcSession session;
    private final Transactions transactions;
    private final Account readOnly;
    private final Account pessimist;
    private final long customerId;
    private final Object lock = new Object();

    SqlOptimistDepositAccount(Account readOnly, Account pessimist, Transactions transactions, JdbcSession session, long customerId) {
        this.readOnly = readOnly;
        this.pessimist = pessimist;
        this.session = session;
        this.transactions = transactions;
        this.customerId = customerId;
    }

    @Override
    public Optional<Balance> deposit(long amount, CharSequence description) {
        synchronized (lock) {
            try {
                final Balance initial = readOnly.balance();
                int actualVersion = initial.version().orElseThrow();
                int newVersion = actualVersion + 1;
                long newBalance = initial.amount() + amount;
                final boolean processed = session
                        .sql("""
                                update saldos set
                                valor = ?,
                                versao = ?
                                where cliente_id = ?
                                and versao = ?""")
                        .set(newBalance)
                        .set(newVersion)
                        .set(customerId)
                        .set(actualVersion)
                        .update(Outcome.UPDATE_COUNT) > 0;
                if (processed) {
                    transactions.add(new Transaction(Optional.of(customerId), amount, 'c', description, Optional.of(Instant.now())));
                    return Optional.of(new Balance(
                            newBalance,
                            initial.limit(),
                            initial.when(),
                            Optional.of(newVersion)
                    ));
                } else {
                    return pessimist.deposit(amount, description);
                }
            } catch (SQLException thrown) {
                logger.log(System.Logger.Level.WARNING, "Fail to optimistic lock deposit", thrown);
                return Optional.empty();
            }
        }
    }

}
