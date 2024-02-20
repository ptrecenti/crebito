package io.amanawa.jdbc;

import java.sql.*;

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
