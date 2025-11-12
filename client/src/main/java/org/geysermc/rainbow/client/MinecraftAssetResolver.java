package org.geysermc.rainbow.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.AtlasManager;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.client.accessor.ResolvedModelAccessor;
import org.geysermc.rainbow.client.mixin.EntityRenderDispatcherAccessor;
import org.geysermc.rainbow.mapping.AssetResolver;
import org.geysermc.rainbow.mapping.texture.TextureResource;
import org.geysermc.rainbow.mixin.SpriteContentsAccessor;

import java.io.InputStream;
import java.util.Optional;

public class MinecraftAssetResolver implements AssetResolver {
    private final ModelManager modelManager;
    private final EquipmentAssetManager equipmentAssetManager;
    private final ResourceManager resourceManager;
    private final AtlasManager atlasManager;

    public MinecraftAssetResolver(Minecraft minecraft) {
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
    public Optional<TextureResource> getTexture(Identifier atlasId, Identifier identifier) {
        if (atlasId == null) {
            // Not in an atlas - so not animated, probably?
            return RainbowIO.safeIO(() -> {
                try (InputStream textureStream = resourceManager.open(Rainbow.decorateTextureIdentifier(identifier))) {
                    return new TextureResource(NativeImage.read(textureStream));
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
        return Optional.of(new TextureResource(textureCopy, sprite.contents().width(), sprite.contents().height()));
    }
}
