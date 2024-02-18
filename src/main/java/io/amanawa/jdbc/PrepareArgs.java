package io.amanawa.jdbc;

import java.sql.*;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;

final class PrepareArgs implements Preparation {

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
