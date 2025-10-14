package org.geysermc.rainbow.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.resources.model.ClientItemInfoLoader;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.geysermc.rainbow.accessor.ResolvedModelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Mixin(ModelManager.class)
public abstract class ModelManagerMixin implements PreparableReloadListener, AutoCloseable, ResolvedModelAccessor {
    @Unique
    private Map<ResourceLocation, ResolvedModel> unbakedResolvedModels;
    @Unique
    private Map<ResourceLocation, ClientItem> clientItems;

    @WrapOperation(method = "method_65753", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;join()Ljava/lang/Object;", ordinal = 1))
    private static Object setResolvedModels(CompletableFuture<?> instance, Operation<Object> original) {
        Object resolved = original.call(instance);
        try {
            // Couldn't be bothered setting up access wideners, this resolves the second component of the ResolvedModels record, which is called "models"
            // Ideally we'd somehow use the "this" instance, but that's not possible here since the lambda we inject into is a static one
            ((ModelManagerMixin) (Object) Minecraft.getInstance().getModelManager()).unbakedResolvedModels = (Map<ResourceLocation, ResolvedModel>) resolved.getClass().getRecordComponents()[1].getAccessor().invoke(resolved);
        } catch (IllegalAccessException | InvocationTargetException | ClassCastException exception) {
            throw new RuntimeException(exception);
        }
        return resolved;
    }

    @WrapOperation(method = "method_65753", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/resources/model/ClientItemInfoLoader$LoadedClientInfos;contents()Ljava/util/Map;"))
    private static Map<ResourceLocation, ClientItem> setClientItems(ClientItemInfoLoader.LoadedClientInfos instance, Operation<Map<ResourceLocation, ClientItem>> original) {
        // Same note as above for not using "this"
        ModelManagerMixin thiz = ((ModelManagerMixin) (Object) Minecraft.getInstance().getModelManager());
        thiz.clientItems = original.call(instance);
        return thiz.clientItems;
    }

    @Override
    public Optional<ResolvedModel> rainbow$getResolvedModel(ResourceLocation location) {
        return unbakedResolvedModels == null ? Optional.empty() : Optional.ofNullable(unbakedResolvedModels.get(location));
    }

    @Override
    public Optional<ClientItem> rainbow$getClientItem(ResourceLocation location) {
        return clientItems == null ? Optional.empty() : Optional.ofNullable(clientItems.get(location));
    }
}
