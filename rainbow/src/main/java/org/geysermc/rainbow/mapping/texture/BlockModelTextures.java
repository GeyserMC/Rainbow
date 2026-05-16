package org.geysermc.rainbow.mapping.texture;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.resources.metadata.animation.FrameSize;
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

public record BlockModelTextures(Map<String, TextureHolder> textures, Map<String, SpriteInfo> sprites, int width, int height, boolean cached) implements ModelTextures<BlockModelTextures> {

    public BlockModelTextures(TextureSlots textures, AssetResolver assets) {
        Map<String, Material> materials = ModelTextures.getCleanMaterials(textures);
        Map<String, TextureHolder> textureHolders = new Object2ObjectOpenHashMap<>();
        Map<String, SpriteInfo> sprites = new Object2ObjectOpenHashMap<>();
        AtomicInteger width = new AtomicInteger();
        AtomicInteger height = new AtomicInteger();

        materials.forEach((key, material) -> {
            assets.getPossibleAtlasTextureSafely(material.sprite()).ifPresent(texture -> {
                try (texture) {
                    textureHolders.put(key, TextureHolder.createBuiltIn(material.sprite()));
                    sprites.put(key, new SpriteInfo(texture));
                    FrameSize size = texture.sizeOfFrame();
                    if (size.width() > width.get()) {
                        width.set(size.width());
                    }
                    if (size.height() > height.get()) {
                        height.set(size.height());
                    }
                }
            });
        });
        this(Collections.unmodifiableMap(textureHolders), Collections.unmodifiableMap(sprites), width.get(), height.get(), false);
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
        return new BlockModelTextures(textures, sprites, width, height, true);
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
