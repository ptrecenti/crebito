package io.amanawa.accounting.jdbc;

import io.amanawa.accounting.Account;
import io.amanawa.accounting.Balance;
import io.amanawa.accounting.Statement;
import io.amanawa.jdbc.JdbcSession;

import javax.sql.DataSource;

public final class ComposedAccount implements Account {

    private final Account readOnly;
    private final Account withdraw;
    private final Account deposit;

    public ComposedAccount(DataSource source, long customerId) {
        this(new ReadOnlyAccount(new JdbcSession(source), customerId), source, customerId);
    }

    private ComposedAccount(Account readOnly, DataSource source, long customerId) {
        this(readOnly,
                new WithdrawAccount(readOnly, new JdbcSession(source), customerId),
                new DepositAccount(readOnly, new JdbcSession(source), customerId));
    }

    private ComposedAccount(Account readOnly, Account withdraw, Account deposit) {
        this.readOnly = readOnly;
        this.withdraw = withdraw;
        this.deposit = deposit;
    }

    @Override
    public boolean withdraw(long amount, CharSequence description) {
        return withdraw.withdraw(amount, description);
    }

    @Override
    public boolean deposit(long amount, CharSequence description) {
        return deposit.deposit(amount, description);
    }

    @Override
    public Balance balance() {
        return readOnly.balance();
    }

    @Override
    public Statement statement() {
        return readOnly.statement();
    }
}
