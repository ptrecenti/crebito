package io.amanawa.accounting.sql;

import io.amanawa.accounting.Account;
import io.amanawa.accounting.Customer;
import io.amanawa.accounting.Transactions;
import io.amanawa.jdbc.JdbcSession;

import java.sql.SQLException;
import java.util.Objects;

final class SqlCustomer implements Customer {

    private final long id;
    private final Account account;
    private final JdbcSession session;
    private final Object lock = new Object();

    public SqlCustomer(JdbcSession session, Transactions transactions, long id) {
        this.id = id;
        this.account = new SqlAccount(session, transactions, id);
        this.session = session;
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
        SqlCustomer that = (SqlCustomer) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
