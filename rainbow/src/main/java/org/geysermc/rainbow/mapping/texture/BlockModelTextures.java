package org.geysermc.rainbow.mapping.texture;

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

public record BlockModelTextures(Map<String, TextureHolder> textures, Map<String, SpriteInfo> sprites, boolean cached) implements ModelTextures<BlockModelTextures> {

    public BlockModelTextures(TextureSlots textures, AssetResolver assets) {
        Map<String, Material> materials = ModelTextures.getCleanMaterials(textures);
        Map<String, TextureHolder> textureHolders = new Object2ObjectOpenHashMap<>();
        Map<String, SpriteInfo> sprites = new Object2ObjectOpenHashMap<>();
        materials.forEach((key, material) -> {
            assets.getPossibleAtlasTextureSafely(material.sprite()).ifPresent(texture -> {
                try (texture) {
                    textureHolders.put(key, TextureHolder.createBuiltIn(material.sprite()));
                    sprites.put(key, new SpriteInfo(texture));
                }
            });
        });
        this(Collections.unmodifiableMap(textureHolders), Collections.unmodifiableMap(sprites), false);
    }

    // Used in GeometryMapper only, should not matter for block geometry?
    @Override
    public int width() {
        return 0;
    }

    @Override
    public int height() {
        return 0;
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
        return new BlockModelTextures(textures, sprites, true);
    }

    @Override
    public CompletableFuture<?> save(PackSerializingContext context) {
        if (!cached) {
            return PackSerializer.Serializable.noop()
                    .with(textures.values())
                    .save(context);
        }
        return PackSerializer.noop();
    }
}
