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

package org.geysermc.rainbow.mapping.geometry;

import com.mojang.math.Transformation;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.resources.Identifier;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.mapping.PackAssetCache;
import org.geysermc.rainbow.mapping.texture.ModelTextures;
import org.geysermc.rainbow.pack.geometry.BedrockGeometry;
import org.geysermc.rainbow.stats.PackStatKeys;

public class MappedGeometryCache extends PackAssetCache<MappedGeometryCache.Key, MappedGeometry> {

    public MappedGeometryCache() {
        super(PackStatKeys.GEOMETRY_CACHE);
    }

    public MappedGeometry mapGeometry(Identifier bedrockIdentifier, ResolvedModel model, Transformation transformation, ModelTextures<?> textures) {
        return getOrCompute(new Key(model, transformation), () -> {
            String safeIdentifier = Rainbow.bedrockSafeIdentifier(bedrockIdentifier);
            BedrockGeometry geometry = GeometryMapper.mapGeometry(safeIdentifier, "bone", model, transformation, textures);
            return new MappedGeometryInstance(geometry);
        });
    }

    public record Key(UnbakedGeometry geometry, Transformation transformation) {

        public Key(ResolvedModel model, Transformation transformation) {
            this(model.getTopGeometry(), transformation);
        }
    }
}
