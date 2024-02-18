package io.amanawa.jdbc;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class JdbcSession {

    private final DataSource source;

    private final Collection<Object> arguments;

    private final Collection<Preparation> preparations;

    private final AtomicReference<Connection> connection;

    private final Object lock;

    private boolean auto;

    private String query;

    public JdbcSession(DataSource source) {
        this.source = source;
        this.arguments = Collections.synchronizedList(new LinkedList<>());
        this.preparations = Collections.synchronizedList(new LinkedList<>());
        this.connection = new AtomicReference<>();
        this.auto = true;
        this.preparations.add(new PrepareArgs(this.arguments));
        this.lock = new Object();
    }

    public JdbcSession sql(String instruction) {
        synchronized (lock) {
            this.query = instruction;
            return this;
        }
    }

    public JdbcSession autoCommit(boolean autoCommit) {
        synchronized (lock) {
            this.auto = autoCommit;
            return this;
        }
    }

    public JdbcSession set(final Object value) {
        synchronized (lock) {
            this.arguments.add(value);
        }
        return this;
    }

    public JdbcSession clear() {
        synchronized (lock) {
            this.arguments.clear();
            this.preparations.clear();
            this.preparations.add(new PrepareArgs(this.arguments));
        }
        return this;
    }

    public void commit() throws SQLException {
        synchronized (lock) {
            final Connection conn = this.connection.get();
            if (conn == null) {
                throw new IllegalStateException(
                        "Connection is not open, can't commit"
                );
            }
            conn.commit();
            this.disconnect();
        }
    }

    public void rollback() throws SQLException {
        synchronized (lock) {
            final Connection conn = this.connection.get();
            if (conn == null) {
                throw new IllegalStateException(
                        "Connection is not open, can't rollback"
                );
            }
            conn.rollback();
            this.disconnect();
        }
    }

    public <T> T insert(final Outcome<T> outcome) throws SQLException {
        synchronized (lock) {
            return this.run(outcome,
                    new Connect.WithKeys(this.query),
                    Request.RUN);
        }
    }

    public <T> JdbcSession updateConcurrent(final Outcome<T> outcome) throws SQLException {
        synchronized (lock) {
            this.run(outcome,
                    new Connect.ConcurUpdateTable(this.query),
                    Request.RUN_QUERY);
            return this;
        }
    }

    public <T> T update(final Outcome<T> outcome) throws SQLException {
        synchronized (lock) {
            return this.run(outcome,
                    new Connect.Plain(this.query),
                    Request.RUN_UPDATE);
        }
    }

    public JdbcSession run() throws SQLException {
        synchronized (lock) {
            final String vendor;
            try (Connection conn = this.source.getConnection()) {
                vendor = conn.getMetaData().getDatabaseProductName();
            }
            final Connect connect;
            if (vendor.equalsIgnoreCase("mysql")) {
                connect = new Connect.WithKeys(this.query);
            } else {
                connect = new Connect.Plain(this.query);
            }
            this.run(Outcome.VOID, connect, Request.RUN);
            return this;
        }
    }

    public <T> T select(final Outcome<T> outcome) throws SQLException {
        synchronized (lock) {
            return this.run(outcome,
                    new Connect.Plain(this.query),
                    Request.RUN_QUERY);
        }
    }


    private <T> T run(final Outcome<T> outcome,
                      final Connect connect, final Request request)
            throws SQLException {
        if (this.query == null) {
            throw new IllegalStateException("Call #sql() first");
        }
        final Connection conn = this.connect();
        conn.setAutoCommit(this.auto);
        try {
            return this.fetch(outcome, request, connect.open(conn));
        } catch (final SQLException ex) {
            this.rollbackOnFailure(conn, ex);
            throw new SQLException(ex);
        } finally {
            if (this.auto) {
                this.disconnect();
            }
            this.clear();
        }
    }

    private <T> T fetch(final Outcome<T> outcome,
                        final Request request, final PreparedStatement stmt) throws SQLException {
        final T result;
        try {
            this.configure(stmt);
            final ResultSet rset = request.fetch(stmt);
            try {
                result = outcome.handle(rset, stmt);
            } finally {
                if (rset != null) {
                    rset.close();
                }
            }
        } finally {
            stmt.close();
        }
        return result;
    }

    private void rollbackOnFailure(final Connection conn, final SQLException failure)
            throws SQLException {
        if (!this.auto) {
            try {
                conn.rollback();
                this.disconnect();
            } catch (final SQLException exc) {
                throw new SQLException(
                        String.format(
                                "Failed to rollback after failure: %s",
                                exc.getMessage()
                        ),
                        failure
                );
            }
        }
    }


    private Connection connect() throws SQLException {
        synchronized (lock) {
            if (this.connection.get() == null) {
                this.connection.set(this.source.getConnection());
            }
            return this.connection.get();
        }
    }

    private void disconnect() throws SQLException {
        final Connection conn = this.connection.getAndSet(null);
        if (conn == null) {
            throw new IllegalStateException(
                    "Connection is not open, can't close"
            );
        }
        conn.close();
    }

    private void configure(final PreparedStatement stmt) throws SQLException {
        for (final Preparation prep : this.preparations) {
            prep.prepare(stmt);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JdbcSession that = (JdbcSession) o;
        return Objects.equals(source, that.source) && Objects.equals(connection, that.connection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, connection);
    }

    interface Request {

        Request RUN = stmt -> {
            stmt.execute();
            return stmt.getGeneratedKeys();
        };

        Request RUN_QUERY = PreparedStatement::executeQuery;

        Request RUN_UPDATE = stmt -> {
            stmt.executeUpdate();
            return stmt.getGeneratedKeys();
        };

        ResultSet fetch(PreparedStatement stmt) throws SQLException;
    }

    interface Connect {

        PreparedStatement open(Connection conn) throws SQLException;

        final class WithKeys implements Connect {
            private final String sql;

            public WithKeys(String sql) {
                this.sql = sql;
            }

            @Override
            public PreparedStatement open(Connection conn) throws SQLException {
                return conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            }
        }

        final class ConcurUpdateTable implements Connect {
            private final String sql;

            ConcurUpdateTable(final String query) {
                this.sql = query;
            }

            @Override
            public PreparedStatement open(final Connection conn) throws SQLException {
                boolean isSupported = conn.getMetaData().supportsResultSetConcurrency(
                        ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
                if (!isSupported) {
                    throw new IllegalStateException("Concurrency not supported");
                }
                return conn.prepareStatement(this.sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
            }
        }

        final class Plain implements Connect {
            private final String sql;

            Plain(final String query) {
                this.sql = query;
            }

            @Override
            public PreparedStatement open(final Connection conn) throws SQLException {
                return conn.prepareStatement(this.sql);
            }
        }
    }

    interface Preparation {
        void prepare(PreparedStatement stmt) throws SQLException;
    }

    static final class PrepareArgs implements Preparation {

        private final Collection<Object> args;

        PrepareArgs(Collection<Object> args) {
            this.args = Collections.unmodifiableCollection(args);
        }

        @Override
        public void prepare(PreparedStatement stmt) throws SQLException {
            int pos = 1;
            for (final Object arg : this.args) {
                if (arg == null) {
                    stmt.setNull(pos, Types.NULL);
                } else if (arg instanceof Long) {
                    stmt.setLong(pos, (Long) arg);
                } else if (arg instanceof Boolean) {
                    stmt.setBoolean(pos, (Boolean) arg);
                } else if (arg instanceof Date) {
                    stmt.setDate(pos, (Date) arg);
                } else if (arg instanceof Integer) {
                    stmt.setInt(pos, (Integer) arg);
                } else if (arg instanceof Instant) {
                    stmt.setTimestamp(pos, Timestamp.from((Instant) arg));
                } else if (arg instanceof Float) {
                    stmt.setFloat(pos, (Float) arg);
                } else if (arg instanceof byte[]) {
                    stmt.setBytes(pos, (byte[]) arg);
                } else {
                    stmt.setObject(pos, arg);
                }
                ++pos;
            }
        }
    }
}
