package org.geysermc.rainbow.datagen.accessor;

import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.Map;

public interface ModelProviderDataAccessor {

    Map<Item, ClientItem> rainbow$getItemInfos();

    Map<Block, BlockModelDefinitionGenerator> rainbow$getBlockDefinitionGenerators();

    Map<Identifier, ModelInstance> rainbow$getModels();
}
