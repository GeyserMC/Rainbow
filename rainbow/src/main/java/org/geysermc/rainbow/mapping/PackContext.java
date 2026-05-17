package org.geysermc.rainbow.mapping;

import org.geysermc.rainbow.definition.GeyserMappings;
import org.geysermc.rainbow.mapping.geometry.GeometryRenderer;
import org.geysermc.rainbow.mapping.geometry.MappedGeometryCache;
import org.geysermc.rainbow.mapping.texture.BlockModelTextures;
import org.geysermc.rainbow.mapping.texture.ItemModelTextures;
import org.geysermc.rainbow.mapping.texture.ModelTextureCache;
import org.geysermc.rainbow.pack.PackPaths;

import java.util.Optional;

// TODO maybe split the responsibilities of this class
public final class PackContext {
    private final GeyserMappings mappings;
    private final PackPaths paths;
    private final BedrockAssetConsumer assetConsumer;
    private final AssetResolver assetResolver;
    private final Optional<GeometryRenderer> geometryRenderer;
    private final boolean reportSuccesses;
    private final ModelTextureCache<BlockModelTextures> blockTextureCache = new ModelTextureCache<>();
    private final ModelTextureCache<ItemModelTextures> itemTextureCache = new ModelTextureCache<>();
    private final MappedGeometryCache geometryCache = new MappedGeometryCache();

    public PackContext(GeyserMappings mappings, PackPaths paths, BedrockAssetConsumer assetConsumer, AssetResolver assetResolver,
                       Optional<GeometryRenderer> geometryRenderer, boolean reportSuccesses) {
        this.mappings = mappings;
        this.paths = paths;
        this.assetConsumer = assetConsumer;
        this.assetResolver = assetResolver;
        this.geometryRenderer = geometryRenderer;
        this.reportSuccesses = reportSuccesses;
    }

    public GeyserMappings mappings() {
        return mappings;
    }

    public PackPaths paths() {
        return paths;
    }

    public BedrockAssetConsumer assetConsumer() {
        return assetConsumer;
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

    public ModelTextureCache<BlockModelTextures> blockTextureCache() {
        return blockTextureCache;
    }

    public ModelTextureCache<ItemModelTextures> itemTextureCache() {
        return itemTextureCache;
    }

    public MappedGeometryCache geometryCache() {
        return geometryCache;
    }

    public AssetCacheStats cacheStats() {
        return new AssetCacheStats(geometryCache.stats(), blockTextureCache.stats(), itemTextureCache.stats());
    }
}
