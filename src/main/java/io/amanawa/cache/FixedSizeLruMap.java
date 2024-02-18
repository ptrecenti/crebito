package io.amanawa.cache;

import java.util.LinkedHashMap;
import java.util.Map;

public final class FixedSizeLruMap<K, V> extends LinkedHashMap<K, V> {

    private final int max;

    public FixedSizeLruMap(int max) {
        this.max = max;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > max;
    }
}
