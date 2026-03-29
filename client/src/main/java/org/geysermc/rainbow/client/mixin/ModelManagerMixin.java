package org.geysermc.rainbow.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.client.resources.model.ClientItemInfoLoader;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.geysermc.rainbow.client.accessor.ResolvedModelAccessor;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;

@Mixin(ModelManager.class)
public abstract class ModelManagerMixin implements PreparableReloadListener, AutoCloseable, ResolvedModelAccessor {
    @Unique
    private @Nullable Map<Identifier, ResolvedModel> unbakedResolvedModels;
    @Unique
    private @Nullable Map<Identifier, ClientItem> clientItems;

    @Inject(method = "discoverModelDependencies", at = @At("TAIL"))
    private static void setResolvedAndItemFields(Map<Identifier, UnbakedModel> allModels, BlockStateModelLoader.LoadedModels blockStateModels, ClientItemInfoLoader.LoadedClientInfos itemInfos,
                                                 // Method returns private record (ResolvedModels)
                                                 @SuppressWarnings("rawtypes") CallbackInfoReturnable callbackInfoReturnable) {
        // Ideally we'd somehow use the "this" instance, but that's not possible here since the method we inject into is a static one
        ModelManagerMixin thiz = ((ModelManagerMixin) (Object) Minecraft.getInstance().getModelManager());

        // Couldn't be bothered setting up access wideners, this resolves the second component of the ResolvedModels record, which is called "models"
        try {
            Object returnValue = callbackInfoReturnable.getReturnValue();
            //noinspection unchecked
            thiz.unbakedResolvedModels = (Map<Identifier, ResolvedModel>) returnValue.getClass().getRecordComponents()[1].getAccessor().invoke(returnValue);
        } catch (InvocationTargetException | IllegalAccessException | ClassCastException exception) {
            throw new RuntimeException(exception);
        }

        thiz.clientItems = itemInfos.contents();
    }

    @Override
    public Optional<ResolvedModel> rainbow$getResolvedModel(Identifier identifier) {
        return unbakedResolvedModels == null ? Optional.empty() : Optional.ofNullable(unbakedResolvedModels.get(identifier));
    }

    @Override
    public Optional<ClientItem> rainbow$getClientItem(Identifier identifier) {
        return clientItems == null ? Optional.empty() : Optional.ofNullable(clientItems.get(identifier));
    }
}
