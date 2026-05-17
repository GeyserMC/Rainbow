package org.geysermc.rainbow.mapping.texture;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
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
            
        }
    }

    @Override
    public CompletableFuture<?> save(PackSerializingContext context) {
        if (!cached) {
            return PackSerializer.Serializable.noop()
                    .with(materials.map(MaterialInfo::texture,
                            map -> map.values().stream()
                                    .map(MaterialInfo::texture)
                                    .collect(PackSerializer.Serializable::noop, PackSerializer.Serializable::with, PackSerializer.Serializable::with)))
                    .save(context);
        }
        return PackSerializer.noop();
    }

    public record MaterialInfo(TextureHolder texture, SpriteInfo sprite, Material material) {

        private MaterialInfo(TextureResource texture, Material material) {
            this(TextureHolder.createBuiltIn(material.sprite()), new SpriteInfo(texture), material);
        }

        private static MaterialInfo createMissing(Material material) {
            return new MaterialInfo(TextureHolder.createNonExistent(material.sprite()), SpriteInfo.EMPTY, material);
        }
    }
}
