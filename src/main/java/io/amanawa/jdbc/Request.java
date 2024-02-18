package io.amanawa.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
