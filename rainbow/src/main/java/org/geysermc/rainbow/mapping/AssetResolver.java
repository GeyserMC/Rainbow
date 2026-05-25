package org.geysermc.rainbow.mapping;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.sounds.SoundEventRegistration;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.level.block.state.BlockState;
import org.geysermc.rainbow.mapping.texture.TextureResource;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

public interface AssetResolver {

    Optional<BlockStateModel.UnbakedRoot> getBlockStateModel(BlockState state);

    Optional<ResolvedModel> getResolvedModel(Identifier identifier);

    Optional<ClientItem> getClientItem(Identifier identifier);

    Optional<EquipmentClientInfo> getEquipmentInfo(ResourceKey<EquipmentAsset> key);

    Optional<TextureResource> getTexture(@Nullable Identifier atlas, Identifier identifier);

    Optional<Resource> getSound(Identifier sound);

    default Optional<TextureResource> getPossibleAtlasTextureSafely(Identifier identifier) {
        // Vanilla behaviour: when baking a Material, check item atlas first, then block atlas
        // (see ModelManager, MaterialBaker)
        return getTexture(AtlasIds.ITEMS, identifier)
                .or(() -> getTexture(AtlasIds.BLOCKS, identifier))
                .or(() -> getTexture(null, identifier));
    }

    Map<String, Map<String, String>> getForeignLanguages();

    Map<String, Map<String, SoundEventRegistration>> getSoundRegistrations();
}
