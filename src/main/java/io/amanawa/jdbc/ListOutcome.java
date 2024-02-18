package io.amanawa.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

public final class ListOutcome<T> implements Outcome<List<T>> {

    private final ListOutcome.Mapping<T> mapping;

    public ListOutcome(Mapping<T> mapping) {
        this.mapping = mapping;
    }

    @Override
    public List<T> handle(ResultSet rset, Statement stmt) throws SQLException {
        final List<T> result = new LinkedList<>();
        while (rset.next()) {
            result.add(this.mapping.map(rset));
        }
        return result;
    }
}
