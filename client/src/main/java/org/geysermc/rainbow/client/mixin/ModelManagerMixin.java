/*
 * Copyright (c) 2026 GeyserMC. https://geysermc.org
 *
 * This file is part of Rainbow.
 *
 * Rainbow is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * Rainbow is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * Rainbow. If not, see <https://www.gnu.org/licenses/>.
 */

package org.geysermc.rainbow.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.client.resources.model.ClientItemInfoLoader;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.level.block.state.BlockState;
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
    private BlockStateModelLoader.@Nullable LoadedModels loadedBlockStateModels;
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
        thiz.loadedBlockStateModels = blockStateModels;

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
    public Optional<BlockStateModel.UnbakedRoot> rainbow$getBlockStateModel(BlockState state) {
        return loadedBlockStateModels == null ? Optional.empty() : Optional.ofNullable(loadedBlockStateModels.models().get(state));
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
