package org.geysermc.rainbow.datagen.mixin;

import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.Map;

public interface ModelProviderDataAccessor {

    Map<Item, ClientItem> rainbow$getItemInfos();

    Map<Identifier, ModelInstance> rainbow$getModels();
}
