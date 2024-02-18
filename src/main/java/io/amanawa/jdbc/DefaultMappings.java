package io.amanawa.jdbc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class DefaultMappings implements Outcome.Mappings {

    private final Map<Class<?>, Outcome.Mapping<?>> map;


    DefaultMappings() {
        this(1);
    }


    DefaultMappings(final int column) {
        this(
                new AbstractMap.SimpleImmutableEntry<>(
                        String.class, rs -> rs.getString(column)
                ),
                new AbstractMap.SimpleImmutableEntry<>(
                        Long.class, rs -> rs.getLong(column)
                ),
                new AbstractMap.SimpleImmutableEntry<>(
                        Boolean.class, rs -> rs.getBoolean(column)
                ),
                new AbstractMap.SimpleImmutableEntry<>(
                        Byte.class, rs -> rs.getByte(column)
                ),
                new AbstractMap.SimpleImmutableEntry<>(
                        Date.class, rs -> rs.getDate(column)
                ),
                new AbstractMap.SimpleImmutableEntry<>(
                        Instant.class, rs -> rs.getTimestamp(column).toInstant()
                ),
                new AbstractMap.SimpleImmutableEntry<>(
                        byte[].class, rs -> rs.getBytes(column)
                ),
                new AbstractMap.SimpleImmutableEntry<>(
                        BigDecimal.class, rs -> rs.getBigDecimal(column)
                ),
                new AbstractMap.SimpleImmutableEntry<>(
                        UUID.class, rs -> rs.getObject(column, UUID.class)
                )
        );
    }

    @SafeVarargs
    private DefaultMappings(
            final Map.Entry<Class<?>, Outcome.Mapping<?>>... mappings
    ) {
        this(Stream.of(mappings).collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue
                        )
                )
        );
    }

    private DefaultMappings(Map<Class<?>, Outcome.Mapping<?>> map) {
        this.map = map;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <X> Outcome.Mapping<X> forType(final Class<? extends X> tpe) {
        if (!this.map.containsKey(tpe)) {
            throw new IllegalArgumentException(
                    String.format("Type %s is not supported", tpe.getName())
            );
        }
        return (Outcome.Mapping<X>) this.map.get(tpe);
    }

}
