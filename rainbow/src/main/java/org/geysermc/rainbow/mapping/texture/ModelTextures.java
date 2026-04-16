package org.geysermc.rainbow.mapping.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.mapping.PackAssetCache;
import org.geysermc.rainbow.mapping.PackContext;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.PackSerializingContext;
import org.geysermc.rainbow.mixin.SpriteContentsAccessor;
import org.geysermc.rainbow.mixin.SpriteLoaderAccessor;
import org.geysermc.rainbow.mixin.TextureSlotsAccessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public interface ModelTextures extends AutoCloseable, PackAssetCache.Cacheable<ModelTextures>, PackSerializer.Serializable {

    ModelTextures EMPTY = new ModelTextures() {
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
            return Optional.empty();
        }

        @Override
        public CompletableFuture<?> save(PackSerializingContext context) {
            return PackSerializer.noop();
        }

        @Override
        public void close() {}
    };

    int width();

    int height();

    Optional<SpriteInfo> getSprite(String key);

    @Override
    default ModelTextures cachedCopy() {
        if (this instanceof CachedTexture) {
            return this;
        }
        return new CachedTexture(this);
    }

    record SpriteInfo(int x, int y, int width, int height) {}

    static ModelTextures load(ResolvedModel model, PackContext context) {
        Map<String, Material> materials = new HashMap<>(((TextureSlotsAccessor) model.getTopTextureSlots()).getResolvedValues());
        materials.remove(UnbakedModel.PARTICLE_TEXTURE_REFERENCE);
        if (materials.size() == 1) {
            Material singleMaterial = materials.values().stream().findAny().orElseThrow();
            return RainbowIO.safeIO(() -> {
                try (TextureResource texture = context.assetResolver().getPossibleAtlasTextureSafely(singleMaterial.sprite()).orElse(null)) {
                    if (texture != null) {
                        return new SingleTexture(texture);
                    }
                }
                return null;
            }, EMPTY);
        }
        Identifier modelIdentifier = Rainbow.getModelIdentifier(model);
        Identifier stitchedTexturesIdentifier = modelIdentifier.withSuffix("_stitched");
        return StitchedTextures.stitchModelTextures(stitchedTexturesIdentifier, materials, context);
    }

    record CachedTexture(ModelTextures delegate) implements ModelTextures {

        @Override
        public int width() {
            return delegate.width();
        }

        @Override
        public int height() {
            return delegate.height();
        }

        @Override
        public Optional<SpriteInfo> getSprite(String key) {
            return delegate.getSprite(key);
        }

        @Override
        public CompletableFuture<?> save(PackSerializingContext context) {
            return PackSerializer.noop();
        }

        @Override
        public void close() {}
    }

    record SingleTexture(SpriteInfo sprite, TextureResource texture) implements ModelTextures {

        public SingleTexture(TextureResource texture) {
            this(new SpriteInfo(0, 0, texture.sizeOfFrame().width(), texture.sizeOfFrame().height()), texture);
        }

        @Override
        public Optional<SpriteInfo> getSprite(String key) {
            return Optional.of(sprite);
        }

        @Override
        public int width() {
            return sprite.width;
        }

        @Override
        public int height() {
            return sprite.height;
        }

        @Override
        public CompletableFuture<?> save(PackSerializingContext context) {
            // TODO FIXME
            return null;
        }

        @Override
        public void close() {
            texture.close();
        }
    }

    record StitchedTextures(Map<String, SpriteInfo> sprites, TextureHolder stitched, int width, int height) implements ModelTextures {
        // Not sure if 16384 should be the max supported texture size, but it seems to work well enough.
        // Max supported texture size seems to mostly be a driver thing, to not let the stitched texture get too big for uploading it to the GPU
        // This is not an issue for us
        private static final int MAX_TEXTURE_SIZE = 1 << 14;

        @Override
        public Optional<SpriteInfo> getSprite(String key) {
            if (TextureSlotsAccessor.invokeIsTextureReference(key)) {
                key = key.substring(1);
            }
            return Optional.ofNullable(sprites.get(key));
        }

        @Override
        public CompletableFuture<?> save(PackSerializingContext context) {
            return stitched.save(context);
        }

        @Override
        public void close() {}

        private static StitchedTextures stitchModelTextures(Identifier stitchedTexturesIdentifier, Map<String, Material> materials, PackContext context) {
            SpriteLoader.Preparations preparations = prepareStitching(materials.values().stream(), context);

            Map<String, SpriteInfo> sprites = new HashMap<>();
            for (Map.Entry<String, Material> material : materials.entrySet()) {
                TextureAtlasSprite sprite = preparations.getSprite(material.getValue().sprite());
                // Sprite could be null when this material wasn't stitched, which happens when the texture simply doesn't exist within the loaded resourcepacks
                if (sprite != null) {
                    sprites.put(material.getKey(), new SpriteInfo(sprite.getX(), sprite.getY(), sprite.contents().width(), sprite.contents().height()));
                }
            }
            return new StitchedTextures(Map.copyOf(sprites), TextureHolder.createCustom(stitchedTexturesIdentifier, () -> stitchTextureAtlas(preparations)), preparations.width(), preparations.height());
        }

        private static SpriteLoader.Preparations prepareStitching(Stream<Material> materials, PackContext context) {
            // TODO - use mixin to clean up SpriteLoader, remove unnecessary bits and bobs for this use, and disable stuff like mipmap and anti-aliasing

            // Atlas ID doesn't matter much here, but ITEMS is the most appropriate (though not always right)
            SpriteLoader spriteLoader = new SpriteLoader(AtlasIds.ITEMS, MAX_TEXTURE_SIZE);
            List<SpriteContents> sprites = materials.distinct()
                    .map(material -> readSpriteContents(material, context))
                    .<SpriteContents>mapMulti(Optional::ifPresent)
                    .toList();
            return  ((SpriteLoaderAccessor) spriteLoader).invokeStitch(sprites, 0, Util.backgroundExecutor());
        }

        private static Optional<SpriteContents> readSpriteContents(Material material, PackContext context) {
            return RainbowIO.safeIO(() -> {
                try (TextureResource texture = context.assetResolver().getPossibleAtlasTextureSafely(material.sprite()).orElse(null)) {
                    if (texture != null) {
                        return new SpriteContents(material.sprite(), texture.sizeOfFrame(), texture.getFirstFrame());
                    }
                }
                return null;
            });
        }

        private static NativeImage stitchTextureAtlas(SpriteLoader.Preparations preparations) {
            NativeImage stitched = new NativeImage(preparations.width(), preparations.height(), true);
            for (TextureAtlasSprite sprite : preparations.regions().values()) {
                try (SpriteContents contents = sprite.contents()) {
                    ((SpriteContentsAccessor) contents).getOriginalImage().copyRect(stitched, 0, 0,
                            sprite.getX(), sprite.getY(), contents.width(), contents.height(), false, false);
                }
            }
            return stitched;
        }
    }
}
