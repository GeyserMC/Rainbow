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
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.resources.Identifier;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.mapping.PackContext;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.PackSerializingContext;
import org.geysermc.rainbow.mapping.animation.AnimationMapper;
import org.geysermc.rainbow.mapping.animation.BedrockAnimationContext;
import org.geysermc.rainbow.mapping.texture.ModelTextures;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public record BedrockGeometryContext(Optional<MappedGeometry> geometry,
                                     Optional<BedrockAnimationContext> animation, boolean handheld) implements PackSerializer.Serializable {
    private static final List<Identifier> HANDHELD_MODELS = Stream.of("item/handheld", "item/handheld_rod", "item/handheld_mace")
            .map(Identifier::withDefaultNamespace)
            .toList();

    @Override
    public CompletableFuture<?> save(PackSerializingContext context) {
        return PackSerializer.Serializable.wrapOptional(geometry)
                .with(animation) // TODO maybe move this out of this
                .save(context);
    }

    public static BedrockGeometryContext create(Identifier bedrockIdentifier, ResolvedModel model, Transformation definitionTransformation, ModelTextures<?> textures, PackContext context) {
        boolean isFlatBuiltin = isFlatBuiltin(model);
        if (isFlatBuiltin) {
            model = new FlatBuiltinItemModel(context.assetResolver(), model);
        }

        ResolvedModel parentModel = model.parent();
        boolean handheld = parentModel != null && HANDHELD_MODELS.contains(Rainbow.getModelIdentifier(parentModel));

        Optional<MappedGeometry> geometry = Optional.empty();
        Optional<BedrockAnimationContext> animation = Optional.empty();

        if (textures.requiresAttachable() || !isFlatBuiltin) {
            // Not flat built-in model, or textures require an attachable (e.g. texture is animated), so map geometry and animation

            geometry = Optional.of(context.geometryCache().mapGeometry(bedrockIdentifier, model, definitionTransformation, textures));
            animation = Optional.of(AnimationMapper.mapAnimation(Rainbow.bedrockSafeIdentifier(bedrockIdentifier), "bone", model.getTopTransforms()));
        }

        return new BedrockGeometryContext(geometry, animation, handheld);
    }

    public static boolean isFlatBuiltin(ResolvedModel model) {
        return !(model.getTopGeometry() instanceof UnbakedCuboidGeometry);
    }
}
