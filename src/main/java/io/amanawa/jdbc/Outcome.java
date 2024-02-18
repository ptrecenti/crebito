package io.amanawa.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public interface Outcome<T> {

    Outcome<Boolean> NOT_EMPTY = (rset, stmt) -> rset.next();

    Outcome<Void> VOID = (rset, stmt) -> Void.TYPE.cast(null);

    Mappings DEFAULT_MAPPINGS = new DefaultMappings();

    T handle(ResultSet rset, Statement stmt) throws SQLException;

    interface Mapping<T> {
        T map(ResultSet rset) throws SQLException;
    }

    interface Mappings {
        <T> Mapping<T> forType(Class<? extends T> tpe);
    }
}