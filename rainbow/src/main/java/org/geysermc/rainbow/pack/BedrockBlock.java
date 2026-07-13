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

package org.geysermc.rainbow.pack;

import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.PackSerializingContext;
import org.geysermc.rainbow.mapping.geometry.MappedGeometry;
import org.geysermc.rainbow.mapping.texture.BlockModelTextures;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public record BedrockBlock(BlockModelTextures textures, Optional<MappedGeometry> geometry) implements PackSerializer.Serializable {

    @Override
    public CompletableFuture<?> save(PackSerializingContext context) {
        return textures
                .with(geometry)
                .save(context);
    }
}
