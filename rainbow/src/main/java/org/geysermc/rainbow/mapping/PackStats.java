package org.geysermc.rainbow.mapping;

public record PackStats(AssetCacheStats cacheStats, int blockMappings, int itemMappings,
                        int waypointStyleMappings,
                        int itemAtlas, int terrainAtlas, int flipbookTextures, int soundDefinitions) {}
