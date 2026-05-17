package org.geysermc.rainbow.mixin;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockStateModel.SimpleCachedUnbakedRoot.class)
public interface BlockStateModelSimpleCachedUnbakedRootAccessor {

    @Accessor
    BlockStateModel.Unbaked getContents();
}
