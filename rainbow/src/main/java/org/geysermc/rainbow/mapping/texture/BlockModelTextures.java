package org.geysermc.rainbow.mapping.texture;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import org.geysermc.rainbow.mapping.AssetResolver;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.PackSerializingContext;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public record BlockModelTextures(Map<String, TextureHolder> textures, Map<String, SpriteInfo> sprites, int width, int height, Optional<Material> singleMaterial,
                                 boolean cached) implements ModelTextures<BlockModelTextures> {

    public BlockModelTextures(TextureSlots textures, AssetResolver assets) {
        Map<String, Material> materials = ModelTextures.getCleanMaterials(textures);
        Map<String, TextureHolder> textureHolders = new Object2ObjectOpenHashMap<>();
        Map<String, SpriteInfo> sprites = new Object2ObjectOpenHashMap<>();
        AtomicInteger width = new AtomicInteger();
        AtomicInteger height = new AtomicInteger();

        Optional<Material> singleMaterial = Optional.empty();
        if (ModelTextures.usesSingleMaterial(materials)) {
            singleMaterial = materials.values().stream().findAny();
            singleMaterial.flatMap(material -> assets.getPossibleAtlasTextureSafely(material.sprite())
                    .map(texture -> Pair.of(material, texture))).ifPresent(materialAndTexture -> {
                try (TextureResource texture = materialAndTexture.getSecond()) {
                    TextureHolder holder = TextureHolder.createBuiltIn(materialAndTexture.getFirst().sprite());
                    SpriteInfo sprite = new SpriteInfo(texture);
                    width.set(sprite.width());
                    height.set(sprite.height());
                    materials.keySet().forEach(key -> {
                        textureHolders.put(key, holder);
                        sprites.put(key, sprite);
                    });
                }
            });
        } else {
            // Don't bother optimising this for distinct textures
            materials.forEach((key, material) -> {
                assets.getPossibleAtlasTextureSafely(material.sprite()).ifPresent(texture -> {
                    try (texture) {
                        textureHolders.put(key, TextureHolder.createBuiltIn(material.sprite()));
                        SpriteInfo sprite = new SpriteInfo(texture);
                        sprites.put(key, sprite);
                        if (sprite.width() > width.get()) {
                            width.set(sprite.width());
                        }
                        if (sprite.height() > height.get()) {
                            height.set(sprite.height());
                        }
                    }
                });
            });
        }

        this(Collections.unmodifiableMap(textureHolders), Collections.unmodifiableMap(sprites), width.get(), height.get(), singleMaterial, false);
    }

    @Override
    public Optional<SpriteInfo> getSprite(String key) {
        return Optional.ofNullable(sprites.get(ModelTextures.sanitizeMaterialReference(key)));
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
        return new BlockModelTextures(textures, sprites, width, height, singleMaterial, true);
    }

    @Override
    public CompletableFuture<?> save(PackSerializingContext context) {
        if (!cached) {
            return PackSerializer.Serializable.noop()
                    .with(textures.values().stream().distinct().toList())
                    .save(context);
        }
        return PackSerializer.noop();
    }
}
