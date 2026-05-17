package org.geysermc.rainbow.datagen.mixin;

import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(ModelProvider.BlockStateGeneratorCollector.class)
public interface BlockStateGeneratorCollectorAccessor {

    @Accessor
    Map<Block, BlockModelDefinitionGenerator> getGenerators();
}
