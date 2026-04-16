package org.geysermc.rainbow.mapping;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

public abstract class PackAssetCache<K, V extends PackAssetCache.Cacheable<V>> {
    private final Map<K, V> cache = new Object2ObjectOpenHashMap<>();

    protected V getOrCompute(K key, Supplier<V> computer) {
        V existing = cache.get(key);
        if (existing != null) {
            return existing.cachedCopy();
        }
        existing = computer.get();
        cache.put(key, existing);
        return existing;
    }

    protected Collection<V> values() {
        return cache.values();
    }

    protected void clear() {
        cache.clear();
    }

    public interface Cacheable<T> {

        T cachedCopy();
    }
}
