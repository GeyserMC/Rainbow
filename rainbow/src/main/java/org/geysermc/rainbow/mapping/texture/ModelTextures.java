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
import net.minecraft.world.item.ItemStackTemplate;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.mapping.PackAssetCache;
import org.geysermc.rainbow.mapping.PackContext;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.PackSerializingContext;
import org.geysermc.rainbow.mapping.geometry.BedrockGeometryContext;
import org.geysermc.rainbow.mixin.SpriteContentsAccessor;
import org.geysermc.rainbow.mixin.SpriteLoaderAccessor;
import org.geysermc.rainbow.mixin.TextureSlotsAccessor;
import org.geysermc.rainbow.pack.attachable.BedrockAttachable;
import org.geysermc.rainbow.pack.rendercontroller.VanillaRenderControllers;
import org.geysermc.rainbow.pack.rendercontroller.BedrockRenderControllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public interface ModelTextures extends PackAssetCache.Cacheable<ModelTextures>, PackSerializer.Serializable {

    int width();

    int height();

    Optional<SpriteInfo> getSprite(String key);

    Identifier icon();

    boolean requiresAttachable();

    default BedrockAttachable.Builder applyToAttachable(BedrockAttachable.Builder builder) {
        return builder
                .withTexture(BedrockAttachable.DisplaySlot.DEFAULT, icon().getPath())
                .withRenderController(VanillaRenderControllers.ITEM_DEFAULT);
    }

    @Override
    default ModelTextures cachedCopy() {
        if (this instanceof CachedTexture) {
            return this;
        }
        return new CachedTexture(this);
    }

    record SpriteInfo(int x, int y, int width, int height) {}

    static ModelTextures load(ItemStackTemplate stack, ResolvedModel model, PackContext context) {
        Map<String, Material> materials = new HashMap<>(((TextureSlotsAccessor) model.getTopTextureSlots()).getResolvedValues());
        materials.remove(UnbakedModel.PARTICLE_TEXTURE_REFERENCE);

        Identifier modelIdentifier = Rainbow.getModelIdentifier(model);

        if (materials.size() == 1) {
            Material singleMaterial = materials.values().stream().findAny().orElseThrow();
            return context.assetResolver().getPossibleAtlasTextureSafely(singleMaterial.sprite())
                    .<ModelTextures>map(texture -> {
                        try (texture) {
                            return new SingleTexture(singleMaterial.sprite(), texture, createIcon(modelIdentifier, stack, materials, context),
                                    BedrockGeometryContext.isFlatBuiltin(model));
                        }
                    })
                    .orElseGet(() -> new MissingTexture(modelIdentifier));
        }

        return StitchedTextures.stitchModelTextures(getStitchedIdentifier(modelIdentifier), materials, createIcon(modelIdentifier, stack, materials, context), context);
    }

    private static TextureHolder createIcon(Identifier identifier, ItemStackTemplate stack, Map<String, Material> materials, PackContext context) {
        // Fallback to trying layer0 when there is no renderer
        return context.geometryRenderer()
                .map(renderer -> renderer.render(identifier, stack))
                .or(() -> Optional.ofNullable(materials.get("layer0"))
                        .map(material -> TextureHolder.createBuiltIn(identifier, material.sprite())))
                .orElseGet(() -> TextureHolder.createNonExistent(identifier));
    }

    private static Identifier getStitchedIdentifier(Identifier identifier) {
        return identifier.withSuffix("_stitched");
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
        public Identifier icon() {
            return delegate.icon();
        }

        @Override
        public boolean requiresAttachable() {
            return delegate.requiresAttachable();
        }

        @Override
        public BedrockAttachable.Builder applyToAttachable(BedrockAttachable.Builder builder) {
            return delegate.applyToAttachable(builder);
        }

        @Override
        public CompletableFuture<?> save(PackSerializingContext context) {
            return PackSerializer.noop();
        }
    }

    record MissingTexture(Identifier icon) implements ModelTextures {

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
        public boolean requiresAttachable() {
            return false;
        }

        @Override
        public CompletableFuture<?> save(PackSerializingContext context) {
            return TextureHolder.createNonExistent(icon).save(context);
        }
    }

    record SingleTexture(SpriteInfo sprite, Identifier texture, ExtractedAnimationInfo animation, TextureHolder iconTexture, boolean flatBuiltinModel) implements ModelTextures {

        public SingleTexture(Identifier texture, TextureResource openedTexture, TextureHolder icon, boolean flatBuiltinModel) {
            this(new SpriteInfo(0, 0, openedTexture.sizeOfFrame().width(), openedTexture.sizeOfFrame().height()), texture,
                    new ExtractedAnimationInfo(openedTexture.totalFrameCount(), openedTexture.frameReferenceCount()), icon, flatBuiltinModel);
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
        public Identifier icon() {
            return iconTexture.location();
        }

        @Override
        public boolean requiresAttachable() {
            return animation.references > 1;
        }

        @Override
        public BedrockAttachable.Builder applyToAttachable(BedrockAttachable.Builder builder) {
            if (animation.references > 1) {
                builder.withRenderController(getRenderControllerIdentifier());
                for (int frame = 0; frame < animation.frames; frame++) {
                    builder.withTexture("frame_" + frame, getFrameIdentifier(frame).getPath());
                }
                return builder;
            } else if (!flatBuiltinModel) {
                // Not flat built-in, so modify attachable similar to StitchedTextures
                return builder
                        .withTexture(BedrockAttachable.DisplaySlot.DEFAULT, ModelTextures.getStitchedIdentifier(texture).getPath())
                        .withRenderController(VanillaRenderControllers.ITEM_DEFAULT);
            }
            return ModelTextures.super.applyToAttachable(builder);
        }

        @Override
        public CompletableFuture<?> save(PackSerializingContext context) {
            // If no animation, just save the texture
            if (animation.references == 1) {
                // Save just the texture (usually layer0) when flat builtin, else save texture and custom icon
                if (flatBuiltinModel) {
                    return TextureHolder.createBuiltIn(texture).save(context);
                }
                // Else, save icon and texture with _stitched suffix
                return iconTexture.with(TextureHolder.createBuiltIn(ModelTextures.getStitchedIdentifier(texture), texture)).save(context);
            }

            // Texture must exist at this point, else a missing texture would've been returned by the load function
            try (TextureResource openedTexture = context.assetResolver().getPossibleAtlasTextureSafely(texture).orElseThrow()) {
                PackSerializer.Serializable serializableStack = iconTexture;

                for (int frame = 0; frame < animation.frames; frame++) {
                    int i = frame;
                    serializableStack = serializableStack.with(TextureHolder.createCustom(getFrameIdentifier(frame), () -> openedTexture.getFrame(i)));
                }

                serializableStack = serializableStack.with(PackSerializer.Serializable.wrapCodec(BedrockRenderControllers.CODEC, createRenderController(openedTexture),
                        paths -> paths.renderControllersPath(getRenderControllerIdentifier())));
                return serializableStack.save(context);
            }
        }

        private Identifier getFrameIdentifier(int index) {
            return texture.withSuffix("_" + index);
        }

        private BedrockRenderControllers createRenderController(TextureResource texture) {
            List<String> textureReferences = createTextureReferenceArray(texture);
            return BedrockRenderControllers.builder()
                    .withRenderController(getRenderControllerIdentifier(), BedrockRenderControllers.renderController("Geometry.default")
                            .withArray(BedrockRenderControllers.RenderProperty.TEXTURES, "Array.frames", textureReferences)
                            .withRenderProperty(BedrockRenderControllers.RenderProperty.MATERIALS, BedrockRenderControllers.DEFAULT_MATERIAL)
                            .withRenderProperty(BedrockRenderControllers.RenderProperty.TEXTURES, createTextureRenderProperty(textureReferences.size())))
                    .build();
        }

        private String getRenderControllerIdentifier() {
            return BedrockRenderControllers.formatRenderControllerName(Rainbow.bedrockSafeIdentifier(iconTexture.location()));
        }

        private static List<String> createTextureReferenceArray(TextureResource texture) {
            List<String> textures = new ArrayList<>();
            for (int frame = 0; frame < texture.frameReferenceCount(); frame++) {
                TextureResource.FrameInfo frameInfo = texture.getFrameInfo(frame);
                // Very poor implementation of the time component, just repeat the frame for however many ticks
                for (int i = 0; i < frameInfo.time(); i++) {
                    textures.add("Texture.frame_" + frameInfo.index());
                }
            }
            return Collections.unmodifiableList(textures);
        }

        private static List<String> createTextureRenderProperty(int frames) {
            return List.of("Array.frames[math.mod(math.floor(q.life_time * 20.0), %d)]".formatted(frames), "Texture.enchanted");
        }

        private record ExtractedAnimationInfo(int frames, int references) {}
    }

    record StitchedTextures(Map<String, SpriteInfo> sprites, TextureHolder stitched, int width, int height, TextureHolder iconTexture) implements ModelTextures {
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
        public Identifier icon() {
            return iconTexture.location();
        }

        @Override
        public boolean requiresAttachable() {
            return true;
        }

        @Override
        public BedrockAttachable.Builder applyToAttachable(BedrockAttachable.Builder builder) {
            return builder
                    .withTexture(BedrockAttachable.DisplaySlot.DEFAULT, stitched.location().getPath())
                    .withRenderController(VanillaRenderControllers.ITEM_DEFAULT);
        }

        @Override
        public CompletableFuture<?> save(PackSerializingContext context) {
            return iconTexture.with(stitched).save(context);
        }

        private static StitchedTextures stitchModelTextures(Identifier stitchedTexturesIdentifier, Map<String, Material> materials, TextureHolder icon, PackContext context) {
            SpriteLoader.Preparations preparations = prepareStitching(materials.values().stream(), context);

            Map<String, SpriteInfo> sprites = new HashMap<>();
            for (Map.Entry<String, Material> material : materials.entrySet()) {
                TextureAtlasSprite sprite = preparations.getSprite(material.getValue().sprite());
                // Sprite could be null when this material wasn't stitched, which happens when the texture simply doesn't exist within the loaded resourcepacks
                if (sprite != null) {
                    sprites.put(material.getKey(), new SpriteInfo(sprite.getX(), sprite.getY(), sprite.contents().width(), sprite.contents().height()));
                }
            }
            return new StitchedTextures(Collections.unmodifiableMap(sprites), TextureHolder.createCustom(stitchedTexturesIdentifier,
                    () -> stitchTextureAtlas(preparations)), preparations.width(), preparations.height(), icon);
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
