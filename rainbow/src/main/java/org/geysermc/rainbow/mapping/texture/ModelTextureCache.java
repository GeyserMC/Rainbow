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

package org.geysermc.rainbow.mapping.texture;

import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.sprite.Material;
import org.geysermc.rainbow.mapping.PackAssetCache;
import org.geysermc.rainbow.mixin.TextureSlotsAccessor;
import org.geysermc.rainbow.stats.PackStatKey;

import java.util.Map;
import java.util.function.Supplier;

public class ModelTextureCache<T extends ModelTextures<T>> extends PackAssetCache<ModelTextureCache.Key, T> {

    public ModelTextureCache(PackStatKey.AssetCacheStatKey statKey) {
        super(statKey);
    }

    public T load(ResolvedModel model, Supplier<T> computer) {
        return getOrCompute(new Key(model), computer);
    }

    public record Key(Map<String, Material> textures) {

        private Key(ResolvedModel model) {
            this(((TextureSlotsAccessor) model.getTopTextureSlots()).getResolvedValues());
        }
    }
}
