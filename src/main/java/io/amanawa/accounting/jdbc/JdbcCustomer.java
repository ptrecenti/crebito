package io.amanawa.accounting.jdbc;

import io.amanawa.accounting.Account;
import io.amanawa.accounting.Customer;
import io.amanawa.jdbc.JdbcSession;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Objects;

public final class JdbcCustomer implements Customer {

    private final long id;
    private final Account account;
    private final JdbcSession session;
    private final Object lock = new Object();

    public JdbcCustomer(DataSource source, long id) {
        this.id = id;
        this.account = new ComposedAccount(source, id);
        this.session = new JdbcSession(source);
    }

    @Override
    public Account account() {
        return account;
    }

    @Override
    public boolean exists() {
        synchronized (lock) {
            try {
                return session.sql("""
                                select id
                                from clientes
                                where id = ?
                                """)
                        .set(id)
                        .select((rset, stmt) -> rset.next());
            } catch (SQLException thrown) {
                throw new IllegalStateException("fail to list customers.", thrown);
            }
        }
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
