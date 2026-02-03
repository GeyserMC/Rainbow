package org.geysermc.rainbow.mapping;

import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.geysermc.rainbow.mapping.texture.TextureResource;

import java.util.Optional;

public interface AssetResolver {

    Optional<ResolvedModel> getResolvedModel(Identifier identifier);

    Optional<ClientItem> getClientItem(Identifier identifier);

    Optional<EquipmentClientInfo> getEquipmentInfo(ResourceKey<EquipmentAsset> key);

    Optional<TextureResource> getTexture(Identifier atlas, Identifier identifier);

    default Optional<TextureResource> getTextureSafely(Identifier atlas, Identifier identifier) {
        if (atlas.equals(ModelManager.BLOCK_OR_ITEM)) {
            return getTexture(AtlasIds.BLOCKS, identifier)
                    .or(() -> getTexture(AtlasIds.ITEMS, identifier));
        }
        return getTexture(atlas, identifier);
    }
}
