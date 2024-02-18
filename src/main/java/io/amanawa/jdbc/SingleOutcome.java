package io.amanawa.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class SingleOutcome<T> implements Outcome<T> {

    private final Mapping<? extends T> mapping;

    private final boolean silently;

    public SingleOutcome(final Class<T> tpe) {
        this(tpe, false);
    }

    public SingleOutcome(final Class<T> tpe, final boolean slnt) {
        this(
                tpe,
                Outcome.DEFAULT_MAPPINGS,
                slnt
        );
    }

    public SingleOutcome(final Class<T> tpe, final Mappings mps, final boolean slnt) {
        this(mps.forType(tpe), slnt);
    }

    public SingleOutcome(Mapping<? extends T> mapping, boolean silently) {
        this.mapping = mapping;
        this.silently = silently;
    }

    @Override
    public T handle(final ResultSet rset, final Statement stmt)
            throws SQLException {
        T result = null;
        if (rset.next()) {
            result = this.fetch(rset);
        } else if (!this.silently) {
            throw new SQLException("No records found");
        }
        return result;
    }

    private T fetch(final ResultSet rset) throws SQLException {
        return this.mapping.map(rset);
    }
}
