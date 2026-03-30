package org.geysermc.rainbow.datagen.mixin;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(DataComponentInitializers.class)
public interface DataComponentInitializersAccessor {

    @Invoker
    Map<ResourceKey<?>, DataComponentMap.Builder> invokeRunInitializers(final HolderLookup.Provider context);
}
