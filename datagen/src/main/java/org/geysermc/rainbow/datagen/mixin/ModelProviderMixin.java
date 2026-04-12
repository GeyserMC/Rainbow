package org.geysermc.rainbow.datagen.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Mixin(ModelProvider.class)
public abstract class ModelProviderMixin implements DataProvider, ModelProviderDataAccessor {
    @Unique
    private @Nullable Map<Item, ClientItem> itemInfos = null;
    @Unique
    private @Nullable Map<Identifier, ModelInstance> models = null;

    @Inject(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/data/models/BlockModelGenerators;run()V"))
    public void setItemInfosAndModels(CachedOutput output, CallbackInfoReturnable<CompletableFuture<?>> callbackInfoReturnable,
                                      @Local(name = "itemModels") ModelProvider.ItemInfoCollector itemInfoCollector,
                                      @Local(name = "simpleModels") ModelProvider.SimpleModelCollector simpleModelCollector) {
        itemInfos = ((ItemInfoCollectorAccessor) itemInfoCollector).getItemInfos();
        models = ((SimpleModelCollectorAccessor) simpleModelCollector).getModels();
    }

    @Override
    public Map<Item, ClientItem> rainbow$getItemInfos() {
        if (itemInfos == null) {
            throw new IllegalStateException("ModelProvider has not run yet");
        }
        return itemInfos;
    }

    @Override
    public Map<Identifier, ModelInstance> rainbow$getModels() {
        if (models == null) {
            throw new IllegalStateException("ModelProvider has not run yet");
        }
        return models;
    }
}
