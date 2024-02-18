package io.amanawa.accounting.jdbc;

import io.amanawa.accounting.Account;
import io.amanawa.accounting.Customer;
import io.amanawa.jdbc.JdbcSession;

import java.util.Objects;

public final class JdbcCustomer implements Customer {

    private final long id;
    private final Account account;

    public JdbcCustomer(JdbcSession session, long id) {
        this.id = id;
        this.account = new JdbcAccount(session, id);
    }

    @Override
    public Account account() {
        return account;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JdbcCustomer that = (JdbcCustomer) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
