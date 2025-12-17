package org.geysermc.rainbow.mapping.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.Material;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.mapping.PackContext;
import org.geysermc.rainbow.mixin.SpriteContentsAccessor;
import org.geysermc.rainbow.mixin.SpriteLoaderAccessor;
import org.geysermc.rainbow.mixin.TextureSlotsAccessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

public record StitchedTextures(Map<String, TextureAtlasSprite> sprites, Supplier<NativeImage> stitched, int width, int height) {
    // Not sure if 16384 should be the max supported texture size, but it seems to work well enough.
    // Max supported texture size seems to mostly be a driver thing, to not let the stitched texture get too big for uploading it to the GPU
    // This is not an issue for us
    private static final int MAX_TEXTURE_SIZE = 1 << 14;

    public Optional<TextureAtlasSprite> getSprite(String key) {
        if (TextureSlotsAccessor.invokeIsTextureReference(key)) {
            key = key.substring(1);
        }
        return Optional.ofNullable(sprites.get(key));
    }

    public static StitchedTextures stitchModelTextures(TextureSlots textures, PackContext context) {
        Map<String, Material> materials = ((TextureSlotsAccessor) textures).getResolvedValues();
        SpriteLoader.Preparations preparations = prepareStitching(materials.values().stream().map(Material::texture), context);

        Map<String, TextureAtlasSprite> sprites = new HashMap<>();
        for (Map.Entry<String, Material> material : materials.entrySet()) {
            TextureAtlasSprite sprite = preparations.getSprite(material.getValue().texture());
            // Sprite could be null when this material wasn't stitched, which happens when the texture simply doesn't exist within the loaded resourcepacks
            if (sprite != null) {
                sprites.put(material.getKey(), sprite);
            }
        }
        return new StitchedTextures(Map.copyOf(sprites), () -> stitchTextureAtlas(preparations), preparations.width(), preparations.height());
    }

    private static SpriteLoader.Preparations prepareStitching(Stream<Identifier> textures, PackContext context) {
        // Atlas ID doesn't matter much here, but BLOCKS is the most appropriate
        SpriteLoader spriteLoader = new SpriteLoader(AtlasIds.BLOCKS, MAX_TEXTURE_SIZE);
        List<SpriteContents> sprites = textures.distinct()
                .map(texture -> readSpriteContents(texture, context))
                .<SpriteContents>mapMulti(Optional::ifPresent)
                .toList();
        return  ((SpriteLoaderAccessor) spriteLoader).invokeStitch(sprites, 0, Util.backgroundExecutor());
    }

    private static Optional<SpriteContents> readSpriteContents(Identifier identifier, PackContext context) {
        return RainbowIO.safeIO(() -> {
            try (TextureResource texture = context.assetResolver().getTexture(AtlasIds.BLOCKS, identifier).orElse(null)) {
                if (texture != null) {
                    return new SpriteContents(identifier, texture.sizeOfFrame(), texture.getFirstFrame(true));
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
