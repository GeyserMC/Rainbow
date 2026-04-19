package org.geysermc.rainbow.mapping;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.Map;
import java.util.function.Supplier;

public abstract class PackAssetCache<K, V extends PackAssetCache.Cacheable<V>> {
    private final Map<K, V> cache = new Object2ObjectOpenHashMap<>();
    private int cacheHits = 0;

    public int cacheSize() {
        return cache.size();
    }

    public int cacheHits() {
        return cacheHits;
    }

    public AssetCacheStats.CacheStats stats() {
        return new AssetCacheStats.CacheStats(cacheSize(), cacheHits);
    }

    protected V getOrCompute(K key, Supplier<V> computer) {
        V existing = cache.get(key);
        if (existing != null) {
            cacheHits++;
            return existing.cachedCopy();
        }
        existing = computer.get();
        cache.put(key, existing);
        return existing;
    }

    protected void clear() {
        cache.clear();
        cacheHits = 0;
    }

    public interface Cacheable<T> {

        T cachedCopy();
    }
}
