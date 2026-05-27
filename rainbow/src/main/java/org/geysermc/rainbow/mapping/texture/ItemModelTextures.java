package org.geysermc.rainbow.mapping.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;
import net.minecraft.world.item.ItemStackTemplate;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.mapping.PackContext;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.PackSerializingContext;
import org.geysermc.rainbow.mapping.geometry.BedrockGeometryContext;
import org.geysermc.rainbow.mixin.SpriteContentsAccessor;
import org.geysermc.rainbow.mixin.SpriteLoaderAccessor;
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
import java.util.function.BiFunction;
import java.util.stream.Stream;

public interface ItemModelTextures extends ModelTextures<ItemModelTextures> {

    IconRenderer icon();

    BedrockAttachable.Builder applyToAttachable(BedrockAttachable.Builder builder);

    @Override
    default ItemModelTextures cachedCopy() {
        if (this instanceof CachedTexture) {
            return this;
        }
        return new CachedTexture(this);
    }

    static ItemModelTextures load(ResolvedModel model, PackContext context) {
        Map<String, Material> materials = ModelTextures.getCleanMaterials(model.getTopTextureSlots());

        Identifier modelIdentifier = Rainbow.getModelIdentifier(model);
        IconRenderer icon = createIcon(modelIdentifier, materials, context);
        boolean isFlatBuiltin = BedrockGeometryContext.isFlatBuiltin(model);

        if (ModelTextures.usesSingleMaterial(materials)) {
            Material singleMaterial = materials.values().stream().findAny().orElseThrow();
            return context.assetResolver().getPossibleAtlasTextureSafely(singleMaterial.sprite())
                    .<ItemModelTextures>map(texture -> {
                        try (texture) {
                            return new SingleTexture(TextureHolder.createBuiltIn(singleMaterial.sprite()), texture, icon, isFlatBuiltin);
                        }
                    })
                    .orElseGet(() -> new MissingTexture(modelIdentifier));
        } else if (isFlatBuiltin) {
            // If using multiple textures and flat built-in, use the icon renderer to properly render an icon
            return new FlatMultiTexture(icon);
        }

        return StitchedTextures.stitchModelTextures(getStitchedIdentifier(modelIdentifier), materials, icon, context);
    }

    private static IconRenderer createIcon(Identifier identifier, Map<String, Material> materials, PackContext context) {
        // Fallback to trying layer0 when there is no renderer
        return context.geometryRenderer()
                .map(renderer -> IconRenderer.stackDependentIcon(
                        (bedrockIdentifier, stack) -> renderer.render(getIconIdentifier(bedrockIdentifier), stack)))
                .or(() -> Optional.ofNullable(materials.get("layer0"))
                        .map(material -> IconRenderer.sharedIcon(TextureHolder.createBuiltIn(getIconIdentifier(identifier), material.sprite()))))
                .orElseGet(() -> IconRenderer.sharedIcon(TextureHolder.createNonExistent(identifier)));
    }

    private static Identifier getIconIdentifier(Identifier bedrockIdentifier) {
        return bedrockIdentifier.withSuffix("_icon");
    }

    private static Identifier getStitchedIdentifier(Identifier identifier) {
        return identifier.withSuffix("_stitched");
    }

    record CachedTexture(ItemModelTextures delegate) implements ItemModelTextures {

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
        public IconRenderer icon() {
            return delegate.icon().copy();
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

    record MissingTexture(IconRenderer icon) implements ItemModelTextures {

        public MissingTexture(Identifier icon) {
            this(IconRenderer.sharedIcon(TextureHolder.createNonExistent(icon)));
        }

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
        public BedrockAttachable.Builder applyToAttachable(BedrockAttachable.Builder builder) {
            return builder;
        }

        @Override
        public CompletableFuture<?> save(PackSerializingContext context) {
            return PackSerializer.noop();
        }
    }

    record SingleTexture(SpriteInfo sprite, TextureHolder texture, Optional<TextureResource.AnimationInfo> animation, IconRenderer iconRenderer, boolean flatBuiltinModel) implements ItemModelTextures {

        public SingleTexture(TextureHolder texture, TextureResource openedTexture, IconRenderer icon, boolean flatBuiltinModel) {
            this(new SpriteInfo(0, 0, openedTexture.sizeOfFrame().width(), openedTexture.sizeOfFrame().height()), texture, openedTexture.animation(), icon, flatBuiltinModel);
        }

        @Override
        public Optional<SpriteInfo> getSprite(String key) {
            return Optional.of(sprite);
        }

        @Override
        public int width() {
            return sprite.width();
        }

        @Override
        public int height() {
            return sprite.height();
        }

        @Override
        public IconRenderer icon() {
            // Save just the texture (usually layer0) when flat builtin, else save texture and custom icon
            return animation.isEmpty() && flatBuiltinModel ? IconRenderer.sharedIcon(texture.copy()) : iconRenderer;
        }

        @Override
        public boolean requiresAttachable() {
            return animation.isPresent();
        }

        @Override
        public BedrockAttachable.Builder applyToAttachable(BedrockAttachable.Builder builder) {
            if (animation.isPresent()) {
                builder.withRenderController(getRenderControllerIdentifier());
                for (int frame = 0; frame < animation.get().totalFrameCount(); frame++) {
                    builder.withTexture("frame_" + frame, TextureHolder.bedrockSafeDestination(getFrameIdentifier(frame)));
                }
                return builder;
            }
            // Not flat built-in, or attachable required somehow, so modify attachable similar to StitchedTextures
            return builder
                    .withTexture(BedrockAttachable.DisplaySlot.DEFAULT, texture)
                    .withRenderController(VanillaRenderControllers.ITEM_DEFAULT);
        }

        @Override
        public CompletableFuture<?> save(PackSerializingContext context) {
            // If no animation, just save the texture
            if (animation.isEmpty()) {
                // Please note that this can lead to a texture saving twice if a mapped block also uses this same texture
                return texture.save(context);
            }

            // Texture must exist at this point, else a missing texture would've been returned by the load function
            try (TextureResource openedTexture = texture.load(context.assetResolver(), context.reporter()).orElseThrow()) {
                PackSerializer.Serializable serializableStack = PackSerializer.Serializable.noop();

                for (int frame = 0; frame < animation.get().totalFrameCount(); frame++) {
                    int i = frame;
                    serializableStack = serializableStack.with(TextureHolder.createCustom(getFrameIdentifier(frame), () -> openedTexture.getFrame(i)));
                }

                serializableStack = serializableStack.with(BedrockRenderControllers.CODEC, createRenderController(animation.get()), paths -> paths.renderControllers(getRenderControllerIdentifier()));
                return serializableStack.save(context);
            }
        }

        private Identifier getFrameIdentifier(int index) {
            return texture.destination.withSuffix("_" + index);
        }

        private BedrockRenderControllers createRenderController(TextureResource.AnimationInfo animation) {
            List<String> textureReferences = createTextureReferenceArray(animation);
            return BedrockRenderControllers.builder()
                    .withRenderController(getRenderControllerIdentifier(), BedrockRenderControllers.renderController("Geometry.default")
                            .withArray(BedrockRenderControllers.RenderProperty.TEXTURES, "Array.frames", textureReferences)
                            .withRenderProperty(BedrockRenderControllers.RenderProperty.MATERIALS, BedrockRenderControllers.DEFAULT_MATERIAL)
                            .withRenderProperty(BedrockRenderControllers.RenderProperty.TEXTURES, createTextureRenderProperty(textureReferences.size())))
                    .build();
        }

        private String getRenderControllerIdentifier() {
            return BedrockRenderControllers.formatRenderControllerName(Rainbow.bedrockSafeIdentifier(texture.destination()));
        }

        private static List<String> createTextureReferenceArray(TextureResource.AnimationInfo animation) {
            List<String> textures = new ArrayList<>();
            for (TextureResource.FrameInfo frame : animation.frames()) {
                // Very poor implementation of the time component, just repeat the frame for however many ticks
                for (int i = 0; i < frame.time(); i++) {
                    textures.add("Texture.frame_" + frame.index());
                }
            }
            return Collections.unmodifiableList(textures);
        }

        private static List<String> createTextureRenderProperty(int frames) {
            return List.of("Array.frames[math.mod(math.floor(q.life_time * 20.0), %d)]".formatted(frames), "Texture.enchanted");
        }
    }

    record FlatMultiTexture(IconRenderer icon) implements ItemModelTextures {

        @Override
        public BedrockAttachable.Builder applyToAttachable(BedrockAttachable.Builder builder) {
            return builder;
        }

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
            // Unsupported - we shouldn't generate geometry for these
            return Optional.empty();
        }

        @Override
        public boolean requiresAttachable() {
            return false;
        }

        @Override
        public CompletableFuture<?> save(PackSerializingContext context) {
            return PackSerializer.noop();
        }
    }

    record StitchedTextures(Map<String, SpriteInfo> sprites, TextureHolder stitched, int width, int height, IconRenderer icon) implements ItemModelTextures {
        // Not sure if 16384 should be the max supported texture size, but it seems to work well enough.
        // Max supported texture size seems to mostly be a driver thing, to not let the stitched texture get too big for uploading it to the GPU
        // This is not an issue for us
        private static final int MAX_TEXTURE_SIZE = 1 << 14;

        @Override
        public Optional<SpriteInfo> getSprite(String key) {
            return Optional.ofNullable(sprites.get(ModelTextures.sanitizeMaterialReference(key)));
        }

        @Override
        public boolean requiresAttachable() {
            return true;
        }

        @Override
        public BedrockAttachable.Builder applyToAttachable(BedrockAttachable.Builder builder) {
            return builder
                    .withTexture(BedrockAttachable.DisplaySlot.DEFAULT, stitched)
                    .withRenderController(VanillaRenderControllers.ITEM_DEFAULT);
        }

        @Override
        public CompletableFuture<?> save(PackSerializingContext context) {
            return stitched.save(context);
        }

        private static StitchedTextures stitchModelTextures(Identifier stitchedTexturesIdentifier, Map<String, Material> materials, IconRenderer icon, PackContext context) {
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

    interface IconRenderer {

        TextureHolder create(Identifier bedrockIdentifier, ItemStackTemplate template);

        IconRenderer copy();

        static IconRenderer sharedIcon(TextureHolder icon) {
            return new IconRenderer() {
                @Override
                public TextureHolder create(Identifier bedrockIdentifier, ItemStackTemplate template) {
                    return icon;
                }

                @Override
                public IconRenderer copy() {
                    TextureHolder copy = icon.copy();
                    return new IconRenderer() {
                        @Override
                        public TextureHolder create(Identifier bedrockIdentifier, ItemStackTemplate template) {
                            return copy;
                        }

                        @Override
                        public IconRenderer copy() {
                            return this;
                        }
                    };
                }
            };
        }

        static IconRenderer stackDependentIcon(BiFunction<Identifier, ItemStackTemplate, TextureHolder> renderer) {
            return new IconRenderer() {
                @Override
                public TextureHolder create(Identifier bedrockIdentifier, ItemStackTemplate template) {
                    return renderer.apply(bedrockIdentifier, template);
                }

                @Override
                public IconRenderer copy() {
                    return this;
                }
            };
        }
    }
}
