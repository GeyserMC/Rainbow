package org.geysermc.rainbow.client;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.client.accessor.ResolvedModelAccessor;
import org.geysermc.rainbow.client.mixin.EntityRenderDispatcherAccessor;
import org.geysermc.rainbow.client.mixin.SpriteContentsAnimatedTextureAccessor;
import org.geysermc.rainbow.client.mixin.SpriteContentsClientAccessor;
import org.geysermc.rainbow.mapping.AssetResolver;
import org.geysermc.rainbow.mapping.texture.TextureResource;
import org.geysermc.rainbow.mixin.SpriteContentsAccessor;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ClientAssetResolver implements AssetResolver {
    private final ModelManager modelManager;
    private final EquipmentAssetManager equipmentAssetManager;
    private final ResourceManager resourceManager;
    private final AtlasManager atlasManager;

    public ClientAssetResolver(Minecraft minecraft) {
        modelManager = minecraft.getModelManager();
        equipmentAssetManager = ((EntityRenderDispatcherAccessor) minecraft.getEntityRenderDispatcher()).getEquipmentAssets();
        resourceManager = minecraft.getResourceManager();
        atlasManager = minecraft.getAtlasManager();
    }

    @Override
    public Optional<ResolvedModel> getResolvedModel(Identifier identifier) {
        return ((ResolvedModelAccessor) modelManager).rainbow$getResolvedModel(identifier);
    }

    @Override
    public Optional<ClientItem> getClientItem(Identifier identifier) {
        return ((ResolvedModelAccessor) modelManager).rainbow$getClientItem(identifier);
    }

    @Override
    public Optional<EquipmentClientInfo> getEquipmentInfo(ResourceKey<EquipmentAsset> key) {
        return Optional.of(equipmentAssetManager.get(key));
    }

    @Override
    public Optional<TextureResource> getTexture(@Nullable Identifier atlasId, Identifier identifier) {
        if (atlasId == null) {
            // Not in an atlas - so not animated, probably?
            return RainbowIO.safeIO(() -> {
                try (InputStream textureStream = resourceManager.open(Rainbow.decorateTextureIdentifier(identifier))) {
                    return TextureResource.createNonAnimated(NativeImage.read(textureStream));
                }
            });
        }
        TextureAtlas atlas = atlasManager.getAtlasOrThrow(atlasId);
        TextureAtlasSprite sprite = atlas.getSprite(identifier);
        if (sprite == atlas.missingSprite()) {
            return Optional.empty();
        }

        NativeImage original = ((SpriteContentsAccessor) sprite.contents()).getOriginalImage();
        NativeImage textureCopy = new NativeImage(original.getWidth(), original.getHeight(), false);
        textureCopy.copyFrom(original);

        SpriteContents.AnimatedTexture animated = ((SpriteContentsClientAccessor) sprite.contents()).getAnimatedTexture();
        if (animated == null) {
            return Optional.of(TextureResource.createNonAnimated(textureCopy));
        }
        SpriteContentsAnimatedTextureAccessor accessor = (SpriteContentsAnimatedTextureAccessor) animated;
        return Optional.of(TextureResource.createAnimated(textureCopy, new FrameSize(sprite.contents().width(), sprite.contents().height()),
                accessor.getFrames().stream()
                        .map(frame -> new TextureResource.FrameInfo(frame.index(), frame.time()))
                        .toList(),
                accessor.getFrameRowSize()));
    }

    @Override
    public Map<String, Map<String, String>> getForeignLanguages() {
        // Ideally we'd not load the language keys again each time, but it's not possible to make use
        // of MC's own language cache here, because MC only loads keys for en_us and the user's language, and,
        // it is not possible to tell which keys are vanilla there.

        Map<Identifier, Resource> languageFiles = resourceManager.listResources("lang", langFile -> langFile.getPath().endsWith(".json"));

        Map<String, Map<String, String>> foreignLanguages = new Object2ObjectOpenHashMap<>();
        for (Map.Entry<Identifier, Resource> languageFile : languageFiles.entrySet()) {
            String packId = languageFile.getValue().sourcePackId();
            // Exclude fabric packs, which have their own translation keys for conventional tags and stuff, which the user likely does not want to include
            if (packId.equals("vanilla") || packId.startsWith("fabric-")) {
                continue;
            }

            String language = languageFile.getKey().getPath();
            language = language.substring(language.lastIndexOf("/") + 1).replaceFirst("\\.json$", "");

            Map<String, String> languageKeys = RainbowIO.safeIO(() -> {
                try (BufferedReader reader = languageFile.getValue().openAsReader()) {
                    return JsonParser.parseReader(reader).getAsJsonObject().asMap().entrySet().stream()
                            .map(translationKey -> Map.entry(translationKey.getKey(), translationKey.getValue().getAsString()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                }
            }, Map.of());

            foreignLanguages.compute(language, (_, existingKeys) -> {
                if (existingKeys == null) {
                    return new Object2ObjectOpenHashMap<>(languageKeys);
                }
                existingKeys.putAll(languageKeys);
                return existingKeys;
            });
        }
        return foreignLanguages;
    }
}
