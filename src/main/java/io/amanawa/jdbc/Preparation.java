package io.amanawa.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

interface Preparation {
    void prepare(PreparedStatement stmt) throws SQLException;
}
