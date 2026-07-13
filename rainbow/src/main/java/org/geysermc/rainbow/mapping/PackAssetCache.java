/*
 * Copyright (c) 2026 GeyserMC. https://geysermc.org
 *
 * This file is part of Rainbow.
 *
 * Rainbow is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * Rainbow is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * Rainbow. If not, see <https://www.gnu.org/licenses/>.
 */

package org.geysermc.rainbow.mapping;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.geysermc.rainbow.stats.PackStatKey;
import org.geysermc.rainbow.stats.PackStats;

import java.util.Map;
import java.util.function.Supplier;

public abstract class PackAssetCache<K, V extends PackAssetCache.Cacheable<V>> implements PackStats.Aggregator {
    private final PackStatKey.AssetCacheStatKey statKey;
    private final Map<K, V> cache = new Object2ObjectOpenHashMap<>();
    private int cacheHits = 0;

    protected PackAssetCache(PackStatKey.AssetCacheStatKey statKey) {
        this.statKey = statKey;
    }

    public int cacheSize() {
        return cache.size();
    }

    public int cacheHits() {
        return cacheHits;
    }

    @Override
    public void collectStats(PackStats.Collector collector) {
        collector.collect(statKey.size(), cacheSize())
                .collect(statKey.hits(), cacheHits);
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
