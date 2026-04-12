package org.geysermc.rainbow.datagen.mixin;

import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FabricLanguageProvider.class)
public interface FabricLanguageProviderAccessor {

    @Accessor
    String getLanguageCode();
}
