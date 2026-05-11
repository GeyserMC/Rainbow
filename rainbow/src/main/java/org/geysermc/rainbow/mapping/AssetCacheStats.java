package org.geysermc.rainbow.mapping;

public record AssetCacheStats(CacheStats geometry, CacheStats texture) {

    public record CacheStats(int size, int hits) {}
}
