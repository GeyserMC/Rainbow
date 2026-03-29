package org.geysermc.rainbow.datagen.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import org.geysermc.rainbow.datagen.RainbowModelProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(ModelProvider.class)
public abstract class ModelProviderMixin implements DataProvider {

    @Inject(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/data/models/BlockModelGenerators;run()V"))
    public void setItemInfosInRainbowModelProvider(CachedOutput output, CallbackInfoReturnable<CompletableFuture<?>> callbackInfoReturnable,
                                                   @Local(name = "itemModels") ModelProvider.ItemInfoCollector itemInfoCollector,
                                                   @Local(name = "simpleModels") ModelProvider.SimpleModelCollector simpleModelCollector) {
        if ((Object) this instanceof RainbowModelProvider rainbowModelProvider) {
            rainbowModelProvider.setItemInfos(((ItemInfoCollectorAccessor) itemInfoCollector).getItemInfos());
            rainbowModelProvider.setModels(((SimpleModelCollectorAccessor) simpleModelCollector).getModels());
        }
    }
}
