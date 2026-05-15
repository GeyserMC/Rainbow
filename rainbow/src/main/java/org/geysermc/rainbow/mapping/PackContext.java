package org.geysermc.rainbow.mapping;

import org.geysermc.rainbow.mapping.geometry.GeometryRenderer;
import org.geysermc.rainbow.definition.item.GeyserItemMappings;
import org.geysermc.rainbow.mapping.geometry.MappedGeometryCache;
import org.geysermc.rainbow.mapping.texture.ModelTextureCache;
import org.geysermc.rainbow.pack.PackPaths;

import java.util.Optional;

// TODO maybe split the responsibilities of this class
public final class PackContext {
    private final GeyserItemMappings mappings;
    private final PackPaths paths;
    private final BedrockItemConsumer itemConsumer;
    private final AssetResolver assetResolver;
    private final Optional<GeometryRenderer> geometryRenderer;
    private final boolean reportSuccesses;
    private final ModelTextureCache textureCache = new ModelTextureCache();
    private final MappedGeometryCache geometryCache = new MappedGeometryCache();

    public PackContext(GeyserItemMappings mappings, PackPaths paths, BedrockItemConsumer itemConsumer, AssetResolver assetResolver,
                       Optional<GeometryRenderer> geometryRenderer, boolean reportSuccesses) {
        this.mappings = mappings;
        this.paths = paths;
        this.itemConsumer = itemConsumer;
        this.assetResolver = assetResolver;
        this.geometryRenderer = geometryRenderer;
        this.reportSuccesses = reportSuccesses;
    }

    public GeyserItemMappings mappings() {
        return mappings;
    }

    public PackPaths paths() {
        return paths;
    }

    public BedrockItemConsumer itemConsumer() {
        return itemConsumer;
    }

    public AssetResolver assetResolver() {
        return assetResolver;
    }

    public Optional<GeometryRenderer> geometryRenderer() {
        return geometryRenderer;
    }

    public boolean reportSuccesses() {
        return reportSuccesses;
    }

    public ModelTextureCache textureCache() {
        return textureCache;
    }

    public MappedGeometryCache geometryCache() {
        return geometryCache;
    }

    public AssetCacheStats cacheStats() {
        return new AssetCacheStats(geometryCache.stats(), textureCache.stats());
    }
}
