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

import com.mojang.blaze3d.platform.Transparency;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import org.geysermc.rainbow.mapping.AssetResolver;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.PackSerializingContext;
import org.geysermc.rainbow.pack.texture.BedrockFlipbookTextures;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public record BlockModelTextures(Either<MaterialInfo, Map<String, MaterialInfo>> materials, int width, int height, boolean cached) implements ModelTextures<BlockModelTextures> {

    public static BlockModelTextures create(TextureSlots textures, AssetResolver assets) {
        Map<String, Material> materials = ModelTextures.getCleanMaterials(textures);

        if (ModelTextures.usesSingleMaterial(materials)) {
            MaterialInfo materialInfo = materials.values().stream().findAny()
                    .map(material -> assets.getPossibleAtlasTextureSafely(material.sprite())
                            .map(texture -> Pair.of(material, texture))
                            .map(materialAndTexture -> {
                                try (TextureResource texture = materialAndTexture.getSecond()) {
                                    return new MaterialInfo(texture, materialAndTexture.getFirst());
                                }
                            })
                            .orElseGet(() -> MaterialInfo.createMissing(material)))
                    .orElseThrow();
            return new BlockModelTextures(Either.left(materialInfo), materialInfo.sprite.width(), materialInfo.sprite.height(), false);
        } else {
            Map<String, MaterialInfo> materialInfos = new Object2ObjectOpenHashMap<>();
            AtomicInteger width = new AtomicInteger();
            AtomicInteger height = new AtomicInteger();

            // Don't bother optimising this for distinct textures
            materials.forEach((key, material) -> {
                assets.getPossibleAtlasTextureSafely(material.sprite()).ifPresent(texture -> {
                    try (texture) {
                        MaterialInfo materialInfo = new MaterialInfo(texture, material);
                        materialInfos.put(key, materialInfo);
                        if (materialInfo.sprite.width() > width.get()) {
                            width.set(materialInfo.sprite.width());
                        }
                        if (materialInfo.sprite.height() > height.get()) {
                            height.set(materialInfo.sprite.height());
                        }
                    }
                });
            });
            return new BlockModelTextures(Either.right(Collections.unmodifiableMap(materialInfos)), width.get(), height.get(), false);
        }
    }

    public boolean isSingleMaterial() {
        return materials.left().isPresent();
    }

    public Optional<MaterialInfo> getSingleMaterial() {
        return materials.left();
    }

    @Override
    public Optional<SpriteInfo> getSprite(String key) {
        return materials.map(material -> Optional.of(material.sprite),
                map -> Optional.ofNullable(map.get(ModelTextures.sanitizeMaterialReference(key))).map(MaterialInfo::sprite));
    }

    @Override
    public boolean requiresAttachable() {
        return false;
    }

    @Override
    public BlockModelTextures cachedCopy() {
        if (cached) {
            return this;
        }
        return new BlockModelTextures(materials, width, height, true);
    }

    public void addFlipbookTextures(BedrockFlipbookTextures.Builder builder) {
        if (!cached) {
            materials.map(info -> info.animation.stream().map(animation -> Pair.of(info.texture, animation)),
                            map -> map.values().stream()
                                    .flatMap(info -> info.animation.stream()
                                            .map(animation -> Pair.of(info.texture, animation))))
                    .map(BlockModelTextures::mapAnimationInfo)
                    .forEach(builder::with);
        }
    }

    private static BedrockFlipbookTextures.FlipbookTexture mapAnimationInfo(Pair<TextureHolder, TextureResource.AnimationInfo> textureAndAnimation) {
        TextureHolder texture = textureAndAnimation.getFirst();
        TextureResource.AnimationInfo animation = textureAndAnimation.getSecond();
        // Same hack as in ItemModelTextures, repeat frame for however long the time is
        IntList frames = IntList.of(animation.frames().stream()
                .flatMap(frame -> Stream.generate(frame::index).limit(frame.time()))
                .mapToInt(i -> i)
                .toArray());
        return new BedrockFlipbookTextures.FlipbookTexture(texture.bedrockSafeName(), texture.bedrockSafeDestination(), 1, frames, animation.frameRowCount(), animation.interpolate());
    }

    @Override
    public CompletableFuture<?> save(PackSerializingContext context) {
        if (!cached) {
            return PackSerializer.Serializable.noop()
                    .with(materials.map(MaterialInfo::texture,
                            map -> PackSerializer.Serializable.allOf(map.values().stream()
                                    .map(MaterialInfo::texture)
                                    .toList())))
                    .save(context);
        }
        return PackSerializer.noop();
    }

    public record MaterialInfo(TextureHolder texture, Optional<TextureResource.AnimationInfo> animation, Transparency transparency, SpriteInfo sprite, Material material) {

        private MaterialInfo(TextureResource texture, Material material) {
            this(TextureHolder.createBuiltIn(material.sprite()), texture.animation(),
                    material.forceTranslucent() ? Transparency.TRANSLUCENT : texture.texture().computeTransparency(),
                    new SpriteInfo(texture), material);
        }

        private static MaterialInfo createMissing(Material material) {
            return new MaterialInfo(TextureHolder.createNonExistent(material.sprite()), Optional.empty(), Transparency.NONE, SpriteInfo.EMPTY, material);
        }
    }
}
