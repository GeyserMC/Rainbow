package org.geysermc.rainbow.mixin;

import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.resources.model.Material;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(TextureSlots.class)
public interface TextureSlotsAccessor {

    @Accessor
    Map<String, Material> getResolvedValues();

    @Invoker
    static boolean invokeIsTextureReference(String name) {
        throw new AssertionError();
    }
}
