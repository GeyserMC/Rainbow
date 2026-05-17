package org.geysermc.rainbow.mapping;

public record AssetCacheStats(CacheStats geometry, CacheStats blockTexture, CacheStats itemTexture) {

    public record CacheStats(int size, int hits) {}
}
