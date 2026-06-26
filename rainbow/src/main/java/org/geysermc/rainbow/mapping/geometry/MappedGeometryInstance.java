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

import org.geysermc.rainbow.mapping.PackSerializingContext;
import org.geysermc.rainbow.pack.geometry.BedrockGeometry;

import java.util.concurrent.CompletableFuture;

public record MappedGeometryInstance(BedrockGeometry geometry) implements MappedGeometry {

    @Override
    public String identifier() {
        return geometry.definitions().getFirst().info().identifier();
    }

    @Override
    public CompletableFuture<?> save(PackSerializingContext context) {
        return context.serializer().saveJson(BedrockGeometry.CODEC, geometry, context.paths().geometry(identifier()));
    }
}
