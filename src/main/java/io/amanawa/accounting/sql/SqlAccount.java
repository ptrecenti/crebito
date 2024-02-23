package io.amanawa.accounting.sql;

import io.amanawa.accounting.Account;
import io.amanawa.accounting.Balance;
import io.amanawa.accounting.Statement;
import io.amanawa.accounting.Transactions;
import io.amanawa.jdbc.JdbcSession;

import java.util.Optional;

final class SqlAccount implements Account {

    private final Account readOnly;
    private final Account withdraw;
    private final Account deposit;
    private final Object lock = new Object();

    public SqlAccount(JdbcSession session, Transactions transactions, long customerId) {
        this(new SqlReadOnlyAccount(session, transactions, customerId), transactions, session, customerId);
    }

    private SqlAccount(Account readOnly, Transactions transaction, JdbcSession session, long customerId) {
        this(readOnly,
                new SqlWithdrawAccount(readOnly, transaction, session, customerId),
                new RetryDepositAccount(new SqlDepositAccount(readOnly, transaction, session, customerId)));
    }

    private SqlAccount(Account readOnly, Account withdraw, Account deposit) {
        this.readOnly = readOnly;
        this.withdraw = withdraw;
        this.deposit = deposit;
    }

    @Override
    public Optional<Balance> withdraw(long amount, CharSequence description) {
        synchronized (lock) {
            return withdraw.withdraw(amount, description);
        }
    }

    @Override
    public Optional<Balance> deposit(long amount, CharSequence description) {
        synchronized (lock) {
            return deposit.deposit(amount, description);
        }
    }

    @Override
    public Balance balance() {
        synchronized (lock) {
            return readOnly.balance();
        }
    }

    @Override
    public Statement statement() {
        synchronized (lock) {
            return readOnly.statement();
        }
    }
}
