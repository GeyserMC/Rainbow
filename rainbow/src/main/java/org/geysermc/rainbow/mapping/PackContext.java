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

import org.geysermc.rainbow.definition.GeyserMappings;
import org.geysermc.rainbow.mapping.geometry.GeometryRenderer;
import org.geysermc.rainbow.mapping.geometry.MappedGeometryCache;
import org.geysermc.rainbow.mapping.texture.BlockModelTextures;
import org.geysermc.rainbow.mapping.texture.ItemModelTextures;
import org.geysermc.rainbow.mapping.texture.ModelTextureCache;
import org.geysermc.rainbow.pack.PackPaths;
import org.geysermc.rainbow.stats.PackStatKeys;

import java.util.Optional;

// TODO maybe split the responsibilities of this class
public final class PackContext {
    private final GeyserMappings mappings;
    private final PackPaths paths;
    private final BedrockAssetConsumer assetConsumer;
    private final AssetResolver assetResolver;
    private final Optional<GeometryRenderer> geometryRenderer;
    private final boolean reportSuccesses;
    private final ModelTextureCache<BlockModelTextures> blockTextureCache = new ModelTextureCache<>(PackStatKeys.BLOCK_TEXTURE_CACHE);
    private final ModelTextureCache<ItemModelTextures> itemTextureCache = new ModelTextureCache<>(PackStatKeys.ITEM_TEXTURE_CACHE);
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
}
