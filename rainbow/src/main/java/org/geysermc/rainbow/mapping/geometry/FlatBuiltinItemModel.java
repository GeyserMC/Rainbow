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

import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.resources.Identifier;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.mapping.AssetResolver;
import org.jspecify.annotations.Nullable;

public class FlatBuiltinItemModel implements ResolvedModel {
    private static final Identifier FLAT_BUILTIN_GENERATED_MODEL = Rainbow.getModdedIdentifier("item/builtin/generated");
    private final ResolvedModel delegate;
    private final ResolvedModel flatBuiltinGeneratedModel;

    public FlatBuiltinItemModel(AssetResolver assetResolver, ResolvedModel delegate) {
        this.delegate = delegate;
        this.flatBuiltinGeneratedModel = assetResolver.getResolvedModel(FLAT_BUILTIN_GENERATED_MODEL).orElseThrow();
    }

    @Override
    public UnbakedModel wrapped() {
        return delegate.wrapped();
    }

    @Override
    public @Nullable ResolvedModel parent() {
        return delegate.parent();
    }

    @Override
    public String debugName() {
        return delegate.debugName();
    }

    @Override
    public UnbakedGeometry getTopGeometry() {
        return flatBuiltinGeneratedModel.getTopGeometry();
    }
}
