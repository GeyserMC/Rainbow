package org.geysermc.rainbow.datagen.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricSoundsProvider;
import net.fabricmc.fabric.impl.datagen.client.SoundTypeBuilderImpl;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import org.geysermc.rainbow.datagen.accessor.SoundsProviderDataAccessor;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.concurrent.CompletionStage;

@Mixin(FabricSoundsProvider.class)
public abstract class FabricSoundsProviderMixin implements DataProvider, SoundsProviderDataAccessor {
    @Unique
    private @Nullable Map<String, Map<String, SoundTypeBuilderImpl.SoundType>> data = null;

    @Inject(method = "lambda$run$0", at = @At("RETURN"))
    public void setData(CachedOutput output, HolderLookup.Provider lookup, CallbackInfoReturnable<CompletionStage> cir,
                        @Local(name = "data") Map<String, Map<String, SoundTypeBuilderImpl.SoundType>> data) {
        this.data = data;
    }

    @Override
    public Map<String, Map<String, SoundTypeBuilderImpl.SoundType>> rainbow$getData() {
        if (data == null) {
            throw new IllegalStateException("FabricSoundsProvider has not run yet");
        }
        return data;
    }
}
